from flask import Flask, render_template, request, jsonify
from flask_cors import CORS
from dotenv import load_dotenv
import os
import json
import requests
import eureka_client
from groq import Groq

app = Flask(__name__)
CORS(app)
load_dotenv()

class InstrumentDatabase:
    def __init__(self):
        self.db = self.load_instrument_data()
        
    def load_instrument_data(self):
        try:
            with open('data/instruments_price_data.json', 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception as e:
            print(f"데이터 로드 실패: {e}")
            return {}
            
    def get_recommendations(self, instrument_type, budget):
        if instrument_type not in self.db:
            return None
            
        instrument_data = self.db[instrument_type]
        
        # 예산에 맞는 가격대 찾기
        if budget <= 500000:
            price_range = "entry"
        elif budget <= 2000000:
            price_range = "intermediate"
        else:
            price_range = "professional"
            
        return {
            "price_range": price_range,
            "range_data": instrument_data.get(price_range, {}),
            "all_ranges": {k: v["range"] for k, v in instrument_data.items()}
        }

class NaverShopAPI:
    def __init__(self, client_id, client_secret):
        self.client_id = client_id
        self.client_secret = client_secret
        self.base_url = "https://openapi.naver.com/v1/search/shop"
        
    def search_product(self, query, display=1):
        headers = {
            "X-Naver-Client-Id": self.client_id,
            "X-Naver-Client-Secret": self.client_secret
        }
        params = {
            "query": query,
            "display": display
        }
        
        try:
            response = requests.get(self.base_url, headers=headers, params=params)
            response.raise_for_status()
            data = response.json()
            return data['items'] if data['items'] else []
        except Exception as e:
            print(f"네이버 쇼핑 API 오류: {e}")
            return []

class InstrumentRecommender:
    def __init__(self, groq_client, naver_shop):
        self.groq_client = groq_client
        self.naver_shop = naver_shop
        self.db = InstrumentDatabase()
        self.instrument_mappings = {
            "일렉": "일렉기타",
            "일렉기타": "일렉기타",
            "베이스": "베이스기타",
            "베이스기타": "베이스기타",
            "피아노": "디지털피아노",
            "전자피아노": "디지털피아노",
            "디지털피아노": "디지털피아노"
        }

    def extract_info_from_message(self, message):
        prompt = f"""다음 메시지에서 악기 종류와 예산을 추출해주세요:
        메시지: "{message}"

        다음 악기 종류 중 하나로만 응답해주세요:
        - 일렉기타
        - 베이스기타
        - 디지털피아노
        
        JSON 형식으로 반환:
        {{"instrument": "악기종류", "budget": "예산(숫자만)"}}"""

        try:
            response = self.groq_client.chat.completions.create(
                model="mixtral-8x7b-32768",
                messages=[
                    {"role": "system", "content": "한국어 메시지에서 악기와 예산 정보를 추출하는 전문가입니다."},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.3
            )
            
            result = response.choices[0].message.content.strip()
            json_str = result[result.find("{"):result.rfind("}")+1]
            return json.loads(json_str)

        except Exception as e:
            print(f"정보 추출 오류: {e}")
            return None

    def get_recommendations(self, instrument_type, budget):
        try:
            budget = int(budget)
        except ValueError:
            budget = 500000

        # 데이터베이스에서 추천 정보 가져오기
        db_recommendations = self.db.get_recommendations(instrument_type, budget)
        if not db_recommendations:
            return {
                'success': False,
                'error': '해당 악기 정보를 찾을 수 없습니다.'
            }

        # RAG 프롬프트 구성
        prompt = f"""악기 전문가로서, {instrument_type} 구매 추천을 해주세요.

        예산: {format(budget, ',')}원

        가격대별 정보:
        {json.dumps(db_recommendations, ensure_ascii=False)}

        위 정보를 바탕으로 예산에 맞는 추천을 해주세요.
        
        다음 형식으로 추천해주세요:
        1. 모델명: [정확한 모델명]
        2. 주요 특징:
           - [특징 1]
           - [특징 2]
           - [특징 3]
        3. 추천 대상: [어떤 사용자에게 적합한지]

        예산 {format(budget, ',')}원 (±10%) 이내의 제품만 추천해주세요.
        반드시 한국어로 답변하고, 한국어로 답변한다는 것은 명시하지 않아도 됩니다.
        실제 구매 가능한 제품만 추천해주세요.
        한국의 ~전문가 말고 bandit의 악기봇으로서~라고 말해주세요"""

        try:
            response = self.groq_client.chat.completions.create(
                model="mixtral-8x7b-32768",
                messages=[
                    {
                        "role": "system",
                        "content": "당신은 한국의 악기 전문가입니다. 반드시 한국어로만 대답해 주세요. 제시된 가격대에 맞는 실제 구매 가능한 제품만 추천해주세요."
                    },
                    {"role": "user", "content": prompt}
                ],
                temperature=0.7
            )
            
            recommendations = response.choices[0].message.content
            
            # 네이버 쇼핑 정보 검색 및 실제 제품 정보 추가
            models = []
            for line in recommendations.split('\n'):
                if '모델명:' in line:
                    model_name = line.split('모델명:')[1].strip()
                    products = self.naver_shop.search_product(model_name)
                    if products:
                        models.append({
                            'name': model_name,
                            'image': products[0].get('image'),
                            'price': products[0].get('lprice'),
                            'link': products[0].get('link'),
                            'mall': products[0].get('mallName')
                        })
            
            return {
                'success': True,
                'recommendations': recommendations,
                'models': models
            }
            
        except Exception as e:
            print(f"추천 생성 오류: {e}")
            return {
                'success': False,
                'error': str(e)
            }

# API 클라이언트 초기화
groq_client = Groq(api_key=os.getenv('GROQ_API_KEY'))
naver_shop = NaverShopAPI(
    client_id=os.getenv('NAVER_CLIENT_ID'),
    client_secret=os.getenv('NAVER_CLIENT_SECRET')
)
recommender = InstrumentRecommender(groq_client, naver_shop)

@app.route('/')
def home():
    return render_template('index.html')

@app.route('/recommend', methods=['POST'])
def recommend():
    try:
        data = request.get_json()
        message = data.get('message', '')
        
        if not message:
            return jsonify({'error': '메시지를 입력해주세요'}), 400
        
        info = recommender.extract_info_from_message(message)
        if not info:
            return jsonify({
                'error': '죄송합니다. 악기 종류와 예산을 좀 더 명확하게 말씀해 주세요.\n예시: "50만원 정도로 어쿠스틱 기타 추천해주세요"'
            }), 400
        
        result = recommender.get_recommendations(info['instrument'], info['budget'])
        
        if not result['success']:
            return jsonify({'error': result['error']}), 500
            
        return jsonify(result)
        
    except Exception as e:
        print(f"추천 오류: {str(e)}", flush=True)
        import traceback
        traceback.print_exc()
        return jsonify({'error': '죄송합니다. 오류가 발생했습니다. 다시 시도해 주세요.'}), 500

if __name__ == '__main__':
    print("유레카 연결", flush=True)
    eureka_client.register_service()
    print("플라스크 실행", flush=True)
    app.run(host='0.0.0.0', port=5000, debug = False)