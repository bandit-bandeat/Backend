import os
import io
import tempfile
import subprocess
from dotenv import load_dotenv
import traceback

from basic_pitch.inference import predict_and_save
from basic_pitch import ICASSP_2022_MODEL_PATH
from basic_pitch.inference import Model
import pretty_midi
from midi2audio import FluidSynth
from pydub import AudioSegment

import boto3

load_dotenv() # .env 파일 불러오기

def mp3_to_midi_text( music_file ):
    print(type(music_file), flush=True)

    # mp3를 미디로 변환한 파일의 경로 받아오기
    midi_path, base_name = mp3_to_midi(music_file)

    print("미디 경로: ", midi_path, flush=True)

    midi_text = midi_to_text(midi_path, base_name)

    return midi_text, base_name


def midi_text_to_mp3( midi_text, base_name ):
    midi_path = text_to_midi(midi_text, base_name)
    mp3_path = midi_to_mp3(midi_path)
    return mp3_path


# 전자 피아노만 만드는 것 같긴 함 -> 기본모델이라 그런지는 모르겠음
def mp3_to_midi(music_file):
    # 음악 임시 저장하는 폴더 생성 -> temp_music으로 만들거고, 없으면 만들거임
    temp_music_dir = './temp_music'
    if not os.path.exists(temp_music_dir):
        os.makedirs(temp_music_dir)


    with tempfile.NamedTemporaryFile(delete=False, suffix='.mp3', dir='./temp_music') as temp_file:
        music_file.save(temp_file)
        music_path = temp_file.name

    # mp3 변환에 사용하는 모델 선언 (basic_pitch 기본 모델 사용)
    model = Model(ICASSP_2022_MODEL_PATH)

    predict_and_save(
        [music_path],
        temp_music_dir,
        True,
        False,
        False,
        False,
        model_or_model_path=model
    )

    # 저장된 미디 파일 경로 생성
    base_name = os.path.splitext(os.path.basename(music_path))[0]
    output_mid = f'temp_music/{base_name}_basic_pitch.mid'

    return output_mid, base_name

def midi_to_text(midi_path, base_name):
    # 저장된 미디 파일 불러오기
    midi_data = pretty_midi.PrettyMIDI(midi_path)

    # 20초 간격으로 미디 텍스트를 저장 (지피티에 다 안들어감)
    midi_texts = []

    # 트랙별로 정보를 추출
    for i, instrument in enumerate(midi_data.instruments) :
        midi_text = []
        midi_text.append(f"Instrument {i+1}: {instrument.program}")

        end_time = 10
        for note in instrument.notes:
            midi_text.append(f"Note: {pretty_midi.note_number_to_name(note.pitch)},"
                             f"Start Time: {note.start}, End Time: {note.end}, Velocity: {note.velocity}")
            # 20초 간격으로 쪼개기
            if note.end > end_time:
                midi_texts.append(midi_text)
                midi_text = []
                end_time += 10
                continue


    temp_music_dir = './temp_music'

    # midi_texts가 이중 배열일 경우, 각 리스트의 항목들을 이어서 리턴
    return ['\n'.join(text) for text in midi_texts]

def text_to_midi(midi_text, base_name):
    # PrettyMIDI 객체 생성
    midi_data = pretty_midi.PrettyMIDI()

    instrument = pretty_midi.Instrument(program=4)

    for line in midi_text.split("\n"):
        if "Note:" in line:  # 노트 정보가 포함된 라인 찾기
            print("지금 바꾸는 노트: ",line , flush=True)
            parts = line.split(",")
            note_name = parts[0].split(":")[1].strip()  # 노트 이름 추출 (예: C4)
            start_time = float(parts[1].split(":")[1].strip())  # 시작 시간
            end_time = float(parts[2].split(":")[1].strip())  # 종료 시간
            velocity = int(parts[3].split(":")[1].strip())  # 강도(velocity)

            # 노트 추가 (PrettyMIDI는 pitch, start_time, end_time, velocity를 받음)
            note = pretty_midi.Note(velocity=velocity, pitch=pretty_midi.note_name_to_number(note_name),
                                    start=start_time, end=end_time)
            instrument.notes.append(note)

    midi_data.instruments.append(instrument)

    midi_data.write(f'./temp_music/{base_name}.mid')
    return f'./temp_music/{base_name}.mid'

def midi_to_mp3(midi_path):
    print("midi_to_mp3 시작", flush=True)
    soundfont_path = download_soundfont_from_s3()
    fs = FluidSynth(sound_font=soundfont_path)

    wav_path = midi_path.replace(".mid", ".wav")  # 변환된 wav 파일 경로
    print("웨이브 경로: ", wav_path, flush=True)

    output_mp3 = midi_path.replace(".mid", "out") # 출력할 mp3 파일 경로
    output_mp3 += ".mp3"
    print("mp3 경로: ", output_mp3, flush=True)

    try:
        print("FluidSynth 실행 전", flush=True)
        fs.midi_to_audio(midi_path, wav_path)
        print(f"WAV 변환 완료: {wav_path}", flush=True)

        audio = AudioSegment.from_wav(wav_path)
        audio.export(output_mp3, format="mp3")
        print(f"MP3 변환 완료: {output_mp3}", flush=True)

        return output_mp3
    except Exception as e:
        print(f"오류 발생: {e}", flush=True)
        traceback.print_exc()

def download_soundfont_from_s3():
    bucket_name = os.getenv("S3_BUCKET_NAME")
    s3_key = os.getenv("S3_KEY")
    # './sound' 디렉토리에 저장할 경로 설정
    local_file_path = './sound/soundfont.sf2'
    # 디렉토리가 없다면 생성
    os.makedirs(os.path.dirname(local_file_path), exist_ok=True)

    # 파일이 이미 존재하면 다운로드하지 않음
    if not os.path.exists(local_file_path):
        s3_client = boto3.client(
            's3',
            aws_access_key_id=os.getenv("S3_ACCESS_KEY"),  # 액세스 키
            aws_secret_access_key=os.getenv("S3_SECRET_KEY"),  # 비밀 키
            region_name=os.getenv("S3_REGION")  # 서울 리전
        )

        # S3에서 파일 다운로드
        s3_client.download_file(bucket_name, s3_key, local_file_path)
        print(f"파일이 다운로드되었습니다: {local_file_path}", flush=True)
    else:
        print(f"파일이 이미 존재합니다: {local_file_path}", flush=True)

    # 로컬 파일 경로 반환
    return local_file_path