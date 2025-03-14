import os
import requests
import json

def register_service():
 
    EUREKA_SERVER_URL = os.getenv("EUREKA_SERVER_URL", "http://18.139.20.145:8761/eureka/apps/")

   
    service_name = "changeService"
    app_id = "flask-app-id"
    instance_id = f"{service_name}-{os.getpid()}"
    host_name = "localhost"
    port = 8080
    vip_address = "flask-service"

  
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
            "healthCheckUrl": f"http://{host_name}:{port}/actuator/health",
            "homePageUrl": f"http://{host_name}:{port}",
            "dataCenterInfo": {
                "@class": "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
                "name": "MyOwn"
            }
        }
    }

    response = requests.post(
        EUREKA_SERVER_URL + service_name,
        headers={"Content-Type": "application/json"},
        data=json.dumps(payload)
    )

    if response.status_code == 200:
        print(f"Service {service_name} registered successfully!")
    else:
        print(f"Failed to register service. Status code: {response.status_code}, {response.text}")
