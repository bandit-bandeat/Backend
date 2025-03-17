from flask import Flask, jsonify, request, Blueprint, abort, send_file, after_this_request
from dotenv import load_dotenv
import os
import logging
import glob
import eureka_client
from models import db, is_change_able
import music_changer

import openai
from llama_index.core import VectorStoreIndex, SimpleDirectoryReader
from llama_index.core import GPTVectorStoreIndex
from llama_index.core import Settings
from llama_index.llms.openai import OpenAI


app = Flask(__name__)

load_dotenv()

ROOT = os.getenv('DB_ROOT')
PASSWORD = os.getenv('DB_PASSWORD')
URL = os.getenv('DB_URL')

print(ROOT, PASSWORD, URL)

app.config['SQLALCHEMY_DATABASE_URI'] = f'mysql+mysqlconnector://{ROOT}:{PASSWORD}@{URL}'
app.config['KEY'] = os.getenv('OPEN_AI_KEY')
openai.api_key = app.config['KEY']

# GPT-4o Mini를 사용
llm = OpenAI(model="gpt-3.5-turbo", temperature=0.7, api_key=app.config['KEY'])
Settings.llm = llm

# 코드 변환 쿼리 엔진
documents = SimpleDirectoryReader('./data/code').load_data()
index = GPTVectorStoreIndex(documents)
query_engin = index.as_query_engine()

# mp3 변환 쿼리 엔진
documents_midi = SimpleDirectoryReader('./data/midi').load_data()
index_midi = GPTVectorStoreIndex(documents)
query_engin_midi = index.as_query_engine()

# db 테이블 연결
db.init_app(app)
# 라우팅
change = Blueprint('change', __name__, url_prefix='/change')
@change.route('/music', methods=['POST'])
def music_change():
    music_file = request.files['music_file']
    style = request.form['style']
    email = request.form['email']
    print(type(music_file))
    print(style)

    if is_change_able(email):
        midi_texts, base_name = music_changer.mp3_to_midi_text(music_file)
        answers = ""
        for i in range(4):
            question = (f'대답은 한글로 해주고,'
                        f'답변은 다른 말 없이, 미디 텍스트만 보여주고, 텍스트 길이는 입력 텍스트랑 동일하게 해줘'
                        f'화음은 각각 한 음으로 표현해줘.\n{midi_texts[i]}\n '
                        f'이 미디 텍스트를 {style}형식으로 바꿔줘\n'
                        f'절대로 화음으로 표현하지 말고, 한 음으로 표현해야 해\n'
                        f'Cmaj7 이런거 쓰지 말고 pretty_midi 라이브러리가 읽을 수 있게 출력해줘\n'
                        f'Note: C2,Start Time: 28.659090909090907, End Time: 28.927272727272726, Velocity: 59 이 형식대로 출력해줘\n'
                        f'그냥 C, D, 이렇게만 하면 안되고, C3 이렇게 적어야 해')
            print(question)
            response = query_engin_midi.query(question)
            #print(response)s
            answer = str(response)
            answers += "\n" + answer
        print("출력 잘 되남: ", answers)
        mp3_path = music_changer.midi_text_to_mp3(answers, base_name)

        # mp3 전송 후, 파일 삭제
        @after_this_request
        def remove_file(response):
            try:
                base_dir = os.path.dirname(mp3_path)
                files_to_delete = glob.glob(os.path.join(base_dir, f"{base_name}*"))
                for file_path in files_to_delete:
                    try:
                        os.remove(file_path)
                        print(f"삭제됨: {file_path}")
                    except Exception as e:
                        print(f"파일 삭제 실패: {file_path}, 오류: {e}")
            except Exception as e:
                print(f'파일 삭제 실패: {e}')
            return response
        # 웹에서 실행하기 어려우면 요청 보내면 다운로드로 바로 되게 설정
        #return send_file(mp3_path, as_attachment=True)
        # 웹에서 mp3 파일 실행 가능하면 이걸로 사용

        return send_file(mp3_path, mimetype="audio/mpeg")

    return jsonify({"error": "하루 질문량을 초과했습니다."}), 400


@change.route('/code', methods=['POST'])
def code_change():
    question = request.json.get('question')
    email = request.json.get('email')
    print(email)
    if is_change_able(email):
        print("질문 가능합니다")
        question = f'대답은 한글로 해줘\n{question}\n 기존 코드랑 바뀐 코드도 보여줘'
        print(question)
        response = query_engin.query(question)
        print(response)
        answer = str(response)
        return jsonify({"answer": answer})
    return jsonify({"error": "하루 질문량을 초과했습니다."}), 400

app.register_blueprint(change)

if __name__ == '__main__':
    print("유레카 연결")
    eureka_client.register_service()
    print("플라스크 실행")
    app.run(host='0.0.0.0', port=5000, debug = False)



