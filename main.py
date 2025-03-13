from flask import Flask, jsonify, request, render_template, Blueprint
from dotenv import load_dotenv
import os
import eureka_client

import openai
from llama_index.core import VectorStoreIndex, SimpleDirectoryReader
from llama_index.core import Document
from llama_index.core import GPTVectorStoreIndex
from llama_index.core import Settings
from llama_index.llms.openai import OpenAI

import music_changer

app = Flask(__name__)

load_dotenv()

app.config['KEY'] = os.getenv('OPEN_AI_KEY')
openai.api_key = app.config['KEY']

# GPT-4o Mini를 사용
llm = OpenAI(model="gpt-4o-mini", temperature=0.7, api_key=app.config['KEY'])
Settings.llm = llm

documents = SimpleDirectoryReader('./data').load_data()
index = GPTVectorStoreIndex(documents)
query_engin = index.as_query_engine()

# 라우팅
change = Blueprint('change', __name__, url_prefix='/change')
@change.route('/music', methods=['POST'])
def music_change():
    music_file = request.files['music_file']
    style = request.form['style']
    print(type(music_file))

    print(music_changer.changer(music_file))
    print(style)
    return "good"

@change.route('/code', methods=['POST'])
def code_change():
    question = request.json.get('question')
    print(question)
    question = f'대답은 한글로 해줘\n{question}\n 기존 코드랑 바뀐 코드도 보여줘'
    print(question)
    response = query_engin.query(question)
    print(response)

    answer = str(response)
    return jsonify( { "answer":answer } )

app.register_blueprint(change)

if __name__ == '__main__':
    print("유레카 연결")
    eureka_client.register_service()
    print("플라스크 실행")
    #app.run(debug=False)
    app.run(host='0.0.0.0', port=5000, debug = False)


