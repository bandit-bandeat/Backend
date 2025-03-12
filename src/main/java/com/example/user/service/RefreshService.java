package com.example.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshService {
    private final RedisTemplate<String, String> redisTemplate;

    public String getRefreshToken(String email) {
        return redisTemplate.opsForValue().get(email);
    }

    public void setRefreshToken(String email, String refreshToken) {
        redisTemplate.opsForValue().set(email, refreshToken);
    }

    public void removeRefreshToken(String email) {
        redisTemplate.delete(email);
    }
}
