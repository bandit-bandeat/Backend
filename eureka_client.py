import requests
import json
import os


def register_service():
    # Eureka 서버 URL 설정
    EUREKA_SERVER_URL = "http://18.139.20.145:8761/eureka/apps/"

    # Flask 애플리케이션 서비스 정보 설정
    service_name = "instrumentService"  # 맞게 수정
    app_id = "flask-app-id"
    instance_id = f"{service_name}-{os.getpid()}"  # 예시로 고유 ID 생성
    host_name = "localhost"  # Flask 서버가 동작하는 호스트
    port = 5000  # Flask 서버 포트 맞게 수정
    vip_address = "flask-service"

    # 서비스 등록 정보
    payload = {
        "instance": {
            "instanceId": instance_id,
            "hostName": host_name,
            "app": service_name,
            "ipAddr": host_name,
            "port": {
                "$": port,
                "@enabled": "true"
            },
            "vipAddress": vip_address,
            "secureVipAddress": vip_address,
            "status": "UP",
            "healthCheckUrl": f"http://{host_name}:{port}/actuator/health",  # 헬스 체크 URL
            "homePageUrl": f"http://{host_name}:{port}",
            "dataCenterInfo": {
                "@class": "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
                "name": "MyOwn"
            }
        }
    }

    # Eureka에 서비스 등록 요청
    response = requests.post(
        EUREKA_SERVER_URL + service_name,
        headers={"Content-Type": "application/json"},
        data=json.dumps(payload)
    )

    if response.status_code == 200:
        print(f"Service {service_name} registered successfully!")
    else:
        print(f"Failed to register service. Status code: {response.status_code}")
