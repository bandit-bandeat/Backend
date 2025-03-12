package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
    @Bean
    public RouteLocator ecomRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 개별 라우트 등록
                // 서비스별 URL 별칭이 1개인 경우, n개인 경우도 존재
                .route("user",
                        r -> r.path("/auth/**").uri("lb://user"))
                .build();
    }
}
