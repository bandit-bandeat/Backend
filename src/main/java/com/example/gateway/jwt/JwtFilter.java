package com.example.gateway.jwt;

import com.example.gateway.entity.User;
import com.example.gateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtFilter implements WebFilter {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if(token == null) { // 로그인 안한 유저는 통과 -> permit all, authenticated로 걸러짐
            return chain.filter(exchange);
        }
        String email = "";
        try{
            email = jwtUtil.getEmail(token);
        }catch(Exception e){
            return setErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "잘못된 토큰입니다.");
        }
        User user = userRepository.findById(email).orElse(null);
        if(user == null)
            return setErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "사용자 계정이 존재하지 않습니다.");

        try{
            jwtUtil.isExpired(token);
        }catch (Exception e){
            return setErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "만료된 토큰입니다.");
        }

        UsernamePasswordAuthenticationToken auth=
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        return chain.filter( exchange.mutate()
                        .request(exchange.getRequest().mutate()
                                .header("X-Auth-User",email).build())
                        .build())
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

    }

    private Mono<Void> setErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String errorResponse = "{\"error\": \"" + message + "\"}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(errorResponse.getBytes())));
    }
}
