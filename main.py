from flask import Flask, jsonify, request, Blueprint, abort
from dotenv import load_dotenv
import os
import eureka_client
from models import db, is_change_able

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
llm = OpenAI(model="gpt-4o-mini", temperature=0.7, api_key=app.config['KEY'])
Settings.llm = llm

documents = SimpleDirectoryReader('./data').load_data()
index = GPTVectorStoreIndex(documents)
query_engin = index.as_query_engine()


# db 테이블 연결
db.init_app(app)
# 라우팅
change = Blueprint('change', __name__, url_prefix='/change')


@change.route('/music', methods=['POST'])
def music_change():
    music_file = request.files['music_file']
    style = request.form['style']
    print(type(music_file))

    #print(music_changer.changer(music_file))
    print(style)
    return "good"

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
    #app.run(debug=False)
    app.run(host='0.0.0.0', port=5000, debug = True)


