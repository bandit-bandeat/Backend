# 환경구성
 터미널 오픈
- 가상환경  web 생성
    - python -m venv web
  - 가상환경 진입
      - 윈도우
          . ./web/Scripts/activate
      - 맥
          source web/bin/activate
  - (가상환경명)프럼프트>
  - 패키지 설치
      pip install flask llama-index openai

# 노래 분위기 바꾸는 코드 진행
- 워크 플로우 1 (mp3를 직접 변환해서 사용자에게 들려주기)
  - 사용자가 바꾸고 싶은 노래 mp3파일과 원하는 장르 입력
  - 업로드한 mp3 파일을 분석하여 mid 파일로 변환
    - Basic Pitch (Spotify) 사용해서 변환
  - 변환한 mid 파일을 텍스트로 변환
    - pretty_midi로 MIDI 파일 텍스트로 변환
  - gpt에게 스타일 변환 요구
    - MIDI 파일 텍스트를 통해 질의
  - 스타일 변경된  MIDI 파일 텍스트를 다시 midㅑ 파일로 변환
    - pretty_midi를 통해  MIDI 파일 텍스트를 midi 파일로 변환
  - mid 파일을 mp3 파일로 변환하여 반환
    - FluidSynth를 통해 midi 파일을 wav 파일로 변환
    - pydub을 통해 wav 파일을 mp3 파일로 변환하여 반환

- 워크 플로우 2 (노래 이름과 분위기만 입력받아서 코드 진행만 텍스트로 보여주기)
  - 사용자가 원하는 노래 이름과 원하는 분위기를 입력
  - GPT를 통해 원하는 코드 스타일 획득
