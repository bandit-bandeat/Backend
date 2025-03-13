from pydub import AudioSegment
from pydub.utils import which
import subprocess
import io

# 시스템에서 FFmpeg과 FFprobe 자동 경로 설정
ffmpeg_path = which("ffmpeg")
ffprobe_path = which("ffprobe")

# 경로가 없다면, FFmpeg을 설치할 위치를 확인하여 오류 처리
if not ffmpeg_path or not ffprobe_path:
    raise EnvironmentError("FFmpeg 또는 FFprobe가 시스템에 설치되지 않았습니다.")

def changer( music_file ):
    print(type(music_file))
    # 업로드 된 파일을 mp3로 변환
    music_data = music_file.read()
    audio = AudioSegment.from_file(io.BytesIO(music_data))
    # BytesIO 객체 생성 (메모리 내에서 바이트 스트림으로 저장)
    wav_bytes = io.BytesIO()
    audio.export(wav_bytes, format="wav")
    # BytesIO 객체를 'asd'와 같은 변수로 사용할 수 있음
    wav_bytes.seek(0)

    return wav_bytes

