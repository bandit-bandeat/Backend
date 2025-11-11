# AI 기반 로고 생성 자동화 (Music → Naming → Logo)

> **프로젝트명**: AI를 활용한 뮤직 & 로고 크리에이션
> **기간**: 2025-02-24 ~ 2025-03-12
> **담당**: 로고 생성 **파이프라인 설계 및 구현** (백엔드/인프라/프론트 연동)

---

## 1) 개요

곡의 **가사(Lyrics)** 를 입력하면, AI가 ① 감성·키워드 분석 → ② **10자 이내 로고 네이밍** → ③ **로고 이미지 자동 생성** → ④ **SVG 벡터화** → ⑤ **S3 업로드 & 실시간 미리보기** 까지를 **원클릭 자동화**로 제공합니다.

* **핵심 가치**: 수동 디자인 없이 즉시 사용 가능한 로고 산출(투명 배경, 고해상도, 확장 가능한 SVG)
* **사용자 경험**: React 미리보기 컴포넌트로 생성→선호도 평가(좋아요/재생성)→재시도까지 일련의 플로우 완결

---

## 2) 워크플로우

1. **가사 분석 → 네이밍**

   * 모델: GPT-4o-mini
   * 제약: **10자 이내**, 의미·분위기 일관성 유지, 한/영 혼용 가능
   * 예시 프롬프트:

   ```text
   아래 가사의 핵심 키워드·분위기를 바탕으로 10자 이내의 로고 이름을 5개 추천해줘.
   - 이름은 간결하고 발음이 쉽고, 브랜드화 가능한 형태로.
   - 너무 일반적인 단어는 지양. 중복·유사성 낮추기.
   - 출력 형식: 1) 네임 2) 간단한 의미
   [가사]
   {{lyrics}}
   ```

2. **로고 이미지 생성**

   * 모델: DALL·E 3
   * 스타일 프롬프트(예):

   ```text
   Minimalistic logo inspired by "{{name}}".
   Clean vector-like shape, strong silhouette, flat design,
   brandable mark, simple geometry, modern typography (no background).
   ```

   * **옵션**: 컬러 팔레트(hex), 글꼴 톤(serif/sans/rounded), 심벌 중심/워드마크 중심, **투명 배경**

3. **후처리 & 포맷 변환**

   * PNG → **SVG 벡터화** (예: ImageTracer/PoTrace 계열, SVGO 최적화)
   * 배경 투명 유지, 크기 표준화(artboard 정리)

4. **스토리지 & 배포**

   * AWS S3: `/logos/{requestId}/logo.png`, `/logos/{requestId}/logo.svg`
   * 공개 URL 반환 → React 미리보기 컴포넌트에 즉시 반영

5. **로그·통계**

   * `AiTable`(MongoDB)에 생성 요청/성공/실패, 선호 옵션, 재생성 이력 저장
   * 대시보드 지표: 생성 요청 수, 성공률, 평균 소요 시간, 선호 색/스타일 Top-N

---

## 3) 기술 스택

| 구분     | 사용 기술                              |
| ------ | ---------------------------------- |
| AI     | OpenAI GPT-4o-mini, DALL·E 3       |
| 백엔드    | **Python (Flask)**, Requests/HTTPX |
| 프론트엔드  | React (미리보기/재생성 버튼/피드백)            |
| 데이터베이스 | MongoDB (`AiTable`)                |
| 스토리지   | AWS S3                             |
| 배포     | (예시) EC2 or Cloud Run, Nginx, PM2  |

---

## 4) API 설계 (예시)

### 4.1 생성 요청

**POST** `/api/logo/generate`

```json
{
  "lyrics": "가사 본문...",
  "options": {
    "palette": ["#2E2E2E", "#FFD400"],
    "fontTone": "sans|serif|rounded",
    "markType": "symbol|wordmark|hybrid",
    "transparent": true
  }
}
```

**Response**

```json
{
  "requestId": "65f...",
  "nameCandidates": [
    {"name": "노바", "note": "새벽/탄생의 이미지"},
    {"name": "에어리" , "note": "가벼움/청명함"}
  ],
  "selectedName": "노바",
  "assets": {
    "pngUrl": "https://s3.../logos/65f.../logo.png",
    "svgUrl": "https://s3.../logos/65f.../logo.svg"
  }
}
```

### 4.2 재생성

**POST** `/api/logo/regenerate`

```json
{ "requestId": "65f...", "selectedName": "노바", "options": { /* 동일 구조 */ } }
```

### 4.3 피드백 기록

**POST** `/api/logo/feedback`

```json
{ "requestId": "65f...", "like": true, "reason": "선호 색상 일치" }
```

---

## 5) 데이터 모델 (MongoDB)

```js
// AiTable (예시)
{
  _id: ObjectId,
  createdAt: Date,
  lyricsHash: String,
  options: {
    palette: [String],
    fontTone: String,
    markType: String,
    transparent: Boolean
  },
  // AI 생성물
  nameCandidates: [{ name: String, note: String }],
  selectedName: String,
  assets: {
    pngUrl: String,
    svgUrl: String
  },
  // 운영 지표
  status: "success"|"failed"|"pending",
  latencyMs: Number,
  error: String,
  feedback: [{ like: Boolean, reason: String, at: Date }]
}
```

* **lyricsHash**: 동일 가사 반복 요청의 캐시/중복 방지에 활용

---

## 6) 환경변수(.env)

```env
# OpenAI
OPENAI_API_KEY=sk-...

# AWS
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
AWS_REGION=ap-northeast-2
S3_BUCKET=ai-logo-bucket

# Mongo
MONGODB_URI=mongodb://localhost:27017/ai_logo

# Server
PORT=8080
ALLOWED_ORIGINS=http://localhost:3000
```

---

## 7) 서버 실행 (로컬)

```bash
# 1) 의존성 설치
pip install -r requirements.txt

# 2) 환경 설정
cp .env.example .env  # 값 채우기

# 3) 실행
python app.py  # 또는 flask run
```

---

## 8) Flask 엔드포인트 스켈레톤 (요약)

```python
from flask import Flask, request, jsonify
import os

app = Flask(__name__)

@app.post('/api/logo/generate')
def generate_logo():
    payload = request.get_json()
    lyrics = payload.get('lyrics', '')
    options = payload.get('options', {})

    # 1) GPT 네이밍 생성
    # 2) DALL·E 이미지 생성
    # 3) 후처리(PNG→SVG)
    # 4) S3 업로드
    # 5) Mongo 기록

    return jsonify({
        "requestId": "...",
        "nameCandidates": [{"name": "노바", "note": "새벽/탄생"}],
        "selectedName": "노바",
        "assets": {"pngUrl": "...", "svgUrl": "..."}
    })

if __name__ == '__main__':
    app.run(port=int(os.getenv('PORT', 8080)), debug=True)
```

---

## 9) React 미리보기 컴포넌트 (예시)

```jsx
import { useState } from 'react'

export default function LogoPreview({ initial }) {
  const [data, setData] = useState(initial)

  return (
    <div style={{display:'grid', gap:16}}>
      <h3>{data.selectedName}</h3>
      <img src={data.assets.pngUrl} alt="logo" style={{width:240, height:'auto'}} />
      <div style={{display:'flex', gap:8}}>
        <a href={data.assets.svgUrl} target="_blank" rel="noreferrer">SVG 보기</a>
        <button onClick={() => fetch('/api/logo/feedback',{method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({requestId:data.requestId, like:true})})}>좋아요</button>
        <button onClick={async()=>{
          const res = await fetch('/api/logo/regenerate',{method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({requestId:data.requestId, selectedName:data.selectedName})})
          const j = await res.json(); setData(j)
        }}>재생성</button>
      </div>
    </div>
  )
}
```

---

## 10) 품질/운영 전략

* **속도**: 요청~배포 **평균 5초 내외** (네트워크/모델 응답에 따라 변동)
* **안정성**: OpenAI 호출 **재시도/백오프**, 부분 실패 시 **정리/롤백**
* **비용 관리**: 입력 토큰 제한, 네이밍 후보 수 동적 조절, 캐시 활용(lyricsHash)
* **보안**: 서버-사이드에서만 OpenAI/AWS 키 사용, S3 퍼블릭 읽기 전용 정책(필요 시 서명 URL)

---

## 11) 트러블슈팅 메모

* SVG 벡터화 시 글자 **외곽선 처리**로 폰트 의존성 제거
* 흰 배경 노이즈가 남을 경우 **threshold/포스터라이즈** 값 조정
* 지나치게 복잡한 프롬프트 → 심볼 과밀/노이즈 증가 → **간결한 스타일 가이드** 유지

---

## 12) 성과 요약

* **자동화**: 텍스트→이미지→업로드 **3단계 완결**
* **속도**: 평균 **5초 이내** 생성/배포
* **유연성**: 팔레트/폰트 톤/마크 타입 등 옵션화
* **확장성**: **SVG 파이프라인**으로 모든 해상도 대응

---

## 13) 로드맵

* 프롬프트 강화(네임 일관성·브랜드 규칙 적용 템플릿)
* 대시보드(Top 색/스타일, 실패 사유, 재생성 패턴)
* 큐(작업량 급증 시) + 비동기 웹훅 콜백
* 사용자 업로드 심벌 학습(커스텀 스타일 가이드)

---

## 14) 라이선스

* 팀/내부 사용 목적. 외부 공개 시 MIT 또는 사내 정책 준수.
