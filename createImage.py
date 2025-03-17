import os
import openai
import boto3
import requests
import eureka_client
from flask import Flask, jsonify, request, Blueprint, abort
from werkzeug.utils import secure_filename
from dotenv import load_dotenv

from llama_index.core import VectorStoreIndex, SimpleDirectoryReader, GPTVectorStoreIndex, Settings
from llama_index.llms.openai import OpenAI

# 환경 변수 로드
load_dotenv()

openai.api_key = os.getenv("OPENAI_API_KEY")
AWS_ACCESS_KEY = os.getenv("AWS_ACCESS_KEY")
AWS_SECRET_KEY = os.getenv("AWS_SECRET_KEY")
AWS_BUCKET_NAME = os.getenv("AWS_BUCKET_NAME")

# Flask 애플리케이션 설정
app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = 'uploads'
os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)

# AWS S3 클라이언트 설정
s3_client = boto3.client(
    's3',
    aws_access_key_id=AWS_ACCESS_KEY,
    aws_secret_access_key=AWS_SECRET_KEY,
    region_name='ap-northeast-3'  # AWS 리전은 환경에 맞게 설정
)

# llama_index 설정
openai_api_key = os.getenv("OPENAI_API_KEY")
llm = OpenAI(model="gpt-4o-mini", temperature=0.7, api_key=openai_api_key)
Settings.llm = llm

documents = SimpleDirectoryReader('./data').load_data()
index = GPTVectorStoreIndex(documents)
query_engine = index.as_query_engine()

# 로고 이름 생성 함수
def generate_logo_name(lyrics):
    try:
        response = openai.ChatCompletion.create(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": "You are an AI that generates creative brand names based on lyrics."},
                {"role": "user", "content": f"Generate a short, catchy logo name based on these lyrics: {lyrics}"}
            ]
        )
        return response["choices"][0]["message"]["content"].strip()
    except Exception as e:
        print(f"Error generating logo name: {e}")
        return "default_logo"

# 이미지 생성 함수
def generate_image(prompt):
    try:
        response = openai.Image.create(
            model="dall-e-3",
            prompt=prompt,
            size="1024x1024",
            quality="standard",
            n=1
        )
        return response['data'][0]['url']
    except Exception as e:
        print(f"Error generating image: {e}")
        return None

# 이미지 다운로드 함수
def download_image(image_url, filename):
    try:
        response = requests.get(image_url)
        response.raise_for_status()
        file_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
        with open(file_path, 'wb') as f:
            f.write(response.content)
        return file_path
    except Exception as e:
        print(f"Error downloading image: {e}")
        return None

# S3에 업로드 함수
def upload_to_s3(file_path, bucket_name, object_name):
    try:
        s3_client.upload_file(file_path, bucket_name, object_name)
        return f'https://{bucket_name}.s3.amazonaws.com/{object_name}'
    except Exception as e:
        print(f"Error uploading to S3: {e}")
        return None

# 쿼리 엔진을 사용하여 질문 처리하는 엔드포인트
@app.route('/query', methods=['POST'])
def process_query():
    data = request.get_json()
    question = data.get('question')

    if not question:
        return jsonify({"error": "질문이 필요합니다."}), 400
    
    try:
        response = query_engine.query(question)
        answer = str(response)
        return jsonify({"answer": answer}), 200
    except Exception as e:
        print(f"Error processing query: {e}")
        return jsonify({"error": "쿼리 처리 중 오류 발생"}), 500

# 로고 생성 API 엔드포인트
@app.route('/generatelogo', methods=['POST'])
def generatelogo():
    data = request.get_json()
    lyrics = data.get('lyrics')

    if not lyrics:
        return jsonify({"error": "노래 가사가 필요합니다."}), 400
    
    logo_name = generate_logo_name(lyrics)
    safe_logo_name = secure_filename(logo_name.replace(" ", "_"))
    image_url = generate_image(f"Minimalistic logo inspired by these lyrics: {lyrics}")

    if not image_url:
        return jsonify({"error": "이미지 생성 실패"}), 500

    image_filename = f"{safe_logo_name}.png"
    local_image_path = download_image(image_url, image_filename)

    if not local_image_path:
        return jsonify({"error": "이미지 다운로드 실패"}), 500

    s3_url = upload_to_s3(local_image_path, AWS_BUCKET_NAME, image_filename)

    if not s3_url:
        return jsonify({"error": "S3 업로드 실패"}), 500

    return jsonify({
        "message": "이미지가 성공적으로 생성되고 S3에 업로드되었습니다.",
        "logo_name": logo_name,
        "s3_url": s3_url
    }), 200

if __name__ == '__main__':
    print("유레카 연결")
    eureka_client.register_service()
    print("플라스크 실행")
    #app.run(debug=False)
    app.run(host='0.0.0.0', port=5000, debug=False)
