import os
import openai
import boto3
import requests
import eureka_client
from flask import Flask, request, jsonify, Blueprint
from werkzeug.utils import secure_filename
from dotenv import load_dotenv
from model import db, is_change_able

# 환경 변수 로드
load_dotenv()

openai.api_key = os.getenv("OPENAI_API_KEY")
AWS_ACCESS_KEY = os.getenv("AWS_ACCESS_KEY")
AWS_SECRET_KEY = os.getenv("AWS_SECRET_KEY")
AWS_BUCKET_NAME = os.getenv("AWS_BUCKET_NAME")
ROOT = os.getenv('DB_ROOT')
PASSWORD = os.getenv('DB_PASSWORD')
URL = os.getenv('DB_URL')
NAME = os.getenv('DB_NAME')

print(ROOT, PASSWORD, URL)

# Flask 애플리케이션 설정
app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = 'uploads'
os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)
app.config['SQLALCHEMY_DATABASE_URI'] = f'mysql+mysqlconnector://{ROOT}:{PASSWORD}@{URL}'
app.config['KEY'] = os.getenv('OPENAI_API_KEY')
openai.api_key = app.config['KEY']

s3_client = boto3.client(
    's3',
    aws_access_key_id=AWS_ACCESS_KEY,
    aws_secret_access_key=AWS_SECRET_KEY,
    region_name='ap-northeast-3' 
)

db.init_app(app)
# 라우팅
change = Blueprint('create', __name__, url_prefix='/create')

# 로고 이름을 짧게 생성하는 함수
def generate_logo_name(lyrics):
    try:
        response = openai.ChatCompletion.create(
            model="gpt-4o-mini",
            messages=[{"role": "system", "content": "You are an AI that generates creative brand names based on lyrics."},
                      {"role": "user", "content": f"Generate a short, catchy logo name in Korean based on these lyrics: {lyrics}"}]
        )
        logo_name = response["choices"][0]["message"]["content"].strip()

        # 로고 이름 길이를 최대 10자로 제한
        short_logo_name = logo_name[:10]  # 예: 10자 이상은 자르기
        return short_logo_name
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
        print("Error generating image:", e, flush=True)
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

@app.route('/generatelogo', methods=['POST'])
def generatelogo():
    data = request.get_json()
    lyrics = data.get('lyrics')
    email = data.get('email')  # 이메일을 요청에서 가져옵니다.

    # 이메일이 유효한지 확인하고, 하루 질문량 초과 여부 확인
    if not is_change_able(email):
        return jsonify({"error": "하루 질문량을 초과했습니다."}), 400

    if not lyrics:
        return jsonify({"error": "노래 가사가 필요합니다."}), 400
    
    # 로고 생성
    logo_name = generate_logo_name(lyrics)
    safe_logo_name = secure_filename(logo_name.replace(" ", "_"))
    image_url = generate_image(f"Minimalistic logo inspired by these lyrics: {lyrics}")

    if not image_url:
        return jsonify({"error": "이미지 생성 실패"}), 500

    image_filename = f"{safe_logo_name}.png"
    local_image_path = download_image(image_url, image_filename)

    if not local_image_path:
        return jsonify({"error": "이미지 다운로드 실패"}), 500

    # S3에 업로드sss
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
    app.run(host='0.0.0.0', port=8080, debug = False)
