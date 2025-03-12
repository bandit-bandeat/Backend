from flask import Flask, jsonify, request, render_template
from dotenv import load_dotenv
import os
import eureka_client

import convert
import openai
from llama_index.core import VectorStoreIndex, SimpleDirectoryReader
from llama_index.core import Document
from llama_index.core import GPTVectorStoreIndex
from llama_index.core import Settings
from llama_index.llms.openai import OpenAI

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
@app.route('/style_change', methods=['POST'])
def style_change():
    music_file = request.files['music_file']
    style = request.form['style']
    print(type(music_file))

    print(style)
    convert.style_change()
    return "good"

@app.route('/code_change', methods=['POST'])
def code_change():
    question = request.json.get('question')
    print(question)
    question = f'대답은 한글로 해줘\n{question}\n 기존 코드랑 바뀐 코드도 보여줘'
    print(question)
    response = query_engin.query(question)
    print(response)

    answer = str(response)
    return jsonify( { "answer":answer } )



if __name__ == '__main__':
    print("유레카 연결")
    eureka_client.register_service()
    print("플라스크 실행")
    app.run(debug=True)


