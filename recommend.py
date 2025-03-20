from flask import Flask, request, jsonify, render_template
from groq import Groq
import os
from dotenv import load_dotenv

app = Flask(__name__)
load_dotenv()

# Groq API 클라이언트 설정
client = Groq(api_key=os.getenv('GROQ_API_KEY')) 

def analyze_and_recommend(songs):
    """
    사용자가 입력한 곡 리스트를 기반으로 공통점을 분석하고 유사한 곡을 추천
    """
    prompt = f"""
    사용자가 입력한 {songs}에 대한 각 곡의 장르, 분위기, 코드 진행, 템포, 악기 구성 등의 공통점을 분석하고, 가장 유사한 다른 곡을 추천해줘.

    ## 입력 곡 리스트:
    {songs}

    ## 분석 방법:
    - 곡들의 장르, 분위기, 코드 진행, 템포, 악기 구성 등의 공통점을 찾아줘.
    - 공통점을 바탕으로 비슷한 특징을 가진 곡을 추천해줘.
    - 단순히 인기 있는 곡이 아니라, 입력된 곡들과 **음악적 요소가 유사한 곡**을 찾아줘.

    ## 출력 형식:
    1. 공통점: 
       - 장르:`
       - 분위기:
       - 코드 진행:
       - 템포:
       - 주요 악기 구성:

    2. 추천 곡 리스트:
       - 곡 제목 - 아티스트 (추천 이유)
       - 곡 제목 - 아티스트 (추천 이유)
       - 곡 제목 - 아티스트 (추천 이유)

    반드시 위의 형식을 지켜서 답변해줘.
    한국어로만 대답 해줘야돼.
    """

    response = client.chat.completions.create(
        messages=[{"role": "system", "content": "you are a helpful assistant."}, 
                  {"role": "user", "content": prompt}],
        model="mixtral-8x7b-32768",
        temperature=0.3,
        stream=False
    )

    return response.choices[0].message.content.strip()

@app.route('/')
def home():
    """웹 페이지 렌더링"""
    return render_template('index.html')

@app.route('/recommend_songs', methods=['POST'])
def recommend_songs():
    """사용자의 곡 리스트를 받아서 추천 곡을 반환"""
    data = request.json
    songs = data.get("songs", [])

    if not songs or not isinstance(songs, list):
        return jsonify({"error": "올바른 곡을 입력하세요."}), 400

    # 곡 분석 및 추천 실행
    result = analyze_and_recommend("\n".join(songs))
    
    return jsonify({"recommendations": result})

if __name__ == '__main__':
    app.run(debug=True, use_reloader=True)
