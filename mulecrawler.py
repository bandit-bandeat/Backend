import requests
from bs4 import BeautifulSoup
import json
from tqdm import tqdm
import random
import time
from collections import Counter

def clean_number(number_string):
    """숫자 문자열에서 쉼표를 제거하고 정수로 변환"""
    try:
        return int(number_string.replace(',', ''))
    except:
        return 0

def crawl_mule_reviews(target_count=1000):
    base_url = "https://www.mule.co.kr/bbs/info/guide"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
    }
    
    all_reviews = []
    session = requests.Session()
    page = 1

    try:
        with tqdm(total=target_count, desc="게시글 수집 진행률") as pbar:
            while len(all_reviews) < target_count:
                # 랜덤 대기 시간 (2~3초)
                time.sleep(random.uniform(2, 3))
                
                response = session.get(base_url, headers=headers, params={'page': page})
                if response.status_code != 200:
                    print(f"\n페이지 {page} 접근 실패")
                    break
                    
                soup = BeautifulSoup(response.text, 'html.parser')
                review_items = soup.select('tr:not(.board-ad-box)')
                
                if not review_items:
                    print("\n더 이상 게시글이 없습니다.")
                    break

                for item in review_items:
                    try:
                        title_td = item.select_one('.thumb-title')
                        if not title_td:
                            continue
                            
                        title_elem = title_td.select_one('.thumb-subject')
                        if not title_elem:
                            continue
                            
                        title = title_elem.text.strip()
                        category = title_td.select_one('.mobile-header-title')
                        category = category.text.strip() if category else "카테고리 없음"
                        
                        view_td = item.select_one('.view')
                        view_count = view_td.text.strip() if view_td else "0"
                        
                        review_data = {
                            'title': title,
                            'category': category,
                            'view_count': clean_number(view_count)
                        }
                        
                        all_reviews.append(review_data)
                        pbar.update(1)
                        
                        if len(all_reviews) >= target_count:
                            break
                        
                    except Exception as e:
                        print(f"\n게시글 파싱 오류: {e}")
                        continue

                # 진행상황 표시
                if len(all_reviews) % 100 == 0:
                    print(f"\n{len(all_reviews)}개 수집 완료")
                
                page += 1

    except Exception as e:
        print(f"\n크롤링 중단: {e}")
        
    finally:
        # 수집된 데이터 저장
        if all_reviews:
            save_data(all_reviews)
    
    return all_reviews

def generate_statistics(reviews):
    """수집된 데이터의 통계 생성"""
    stats = {
        "total_reviews": len(reviews),
        "total_views": sum(review['view_count'] for review in reviews),
        "categories": {},
        "top_viewed": [],
        "average_views": 0
    }
    
    # 카테고리별 통계
    category_counter = Counter(review['category'] for review in reviews)
    for category, count in category_counter.most_common():
        category_reviews = [r for r in reviews if r['category'] == category]
        category_views = sum(r['view_count'] for r in category_reviews)
        
        stats["categories"][category] = {
            "count": count,
            "total_views": category_views,
            "average_views": round(category_views / count if count > 0 else 0, 2)
        }
    
    # 조회수 기준 상위 10개 게시글
    top_reviews = sorted(reviews, key=lambda x: x['view_count'], reverse=True)[:10]
    stats["top_viewed"] = [
        {
            "title": review['title'],
            "category": review['category'],
            "view_count": review['view_count']
        }
        for review in top_reviews
    ]
    
    stats["average_views"] = round(stats["total_views"] / len(reviews) if reviews else 0, 2)
    
    return stats

def save_data(reviews):
    """데이터를 JSON 파일로 저장"""
    try:
        # 리뷰 데이터 저장
        with open('mule_reviews_1000.json', 'w', encoding='utf-8') as f:
            json.dump(reviews, f, ensure_ascii=False, indent=2)
        print("\n리뷰 데이터 저장 완료")
        
        # 통계 생성 및 저장
        stats = generate_statistics(reviews)
        with open('mule_reviews_stats.json', 'w', encoding='utf-8') as f:
            json.dump(stats, f, ensure_ascii=False, indent=2)
        print("통계 데이터 저장 완료")
        
    except Exception as e:
        print(f"\n저장 오류: {e}")

def print_statistics(stats):
    """통계 정보 출력"""
    print("\n=== 수집 통계 ===")
    print(f"총 게시글: {stats['total_reviews']}개")
    print(f"총 조회수: {stats['total_views']:,}회")
    print(f"평균 조회수: {stats['average_views']:,.1f}회")
    
    print("\n=== 카테고리별 통계 ===")
    for category, data in sorted(stats['categories'].items(), 
                               key=lambda x: x[1]['count'], reverse=True):
        print(f"\n{category}:")
        print(f"  게시글 수: {data['count']}개")
        print(f"  총 조회수: {data['total_views']:,}회")
        print(f"  평균 조회수: {data['average_views']:,.1f}회")
    
    print("\n=== 인기 게시글 (상위 5개) ===")
    for i, post in enumerate(stats['top_viewed'][:5], 1):
        print(f"\n{i}. {post['title']}")
        print(f"   조회수: {post['view_count']:,}회")
        print(f"   카테고리: {post['category']}")

def main():
    print("뮬 리뷰 수집 시작...")
    reviews = crawl_mule_reviews(1000)
    
    if reviews:
        stats = generate_statistics(reviews)
        print_statistics(stats)
        print(f"\n총 수집된 게시글: {len(reviews)}개")

if __name__ == "__main__":
    main()