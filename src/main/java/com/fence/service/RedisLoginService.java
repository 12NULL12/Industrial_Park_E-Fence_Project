package com.fence.service;
//wzj
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisLoginService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String TOKEN_PREFIX = "token:";
    private static final String USER_TOKEN_PREFIX = "user_token:";
    private static final long EXPIRE_SECONDS = 7 * 24 * 60 * 60; // 7天

    /**
     * 生成Token并存储
     */
    public String createToken(String userId) {
        String token = UUID.randomUUID().toString().replace("-", "");

        // 存 Token -> UserId
        redisTemplate.opsForValue().set(TOKEN_PREFIX + token, userId, EXPIRE_SECONDS, TimeUnit.SECONDS);

        // 存 UserId -> Token（多端登录）
        redisTemplate.opsForSet().add(USER_TOKEN_PREFIX + userId, token);
        redisTemplate.expire(USER_TOKEN_PREFIX + userId, EXPIRE_SECONDS, TimeUnit.SECONDS);

        return token;
    }

    /**
     * 验证Token
     */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_PREFIX + token));
    }

    /**
     * 根据Token获取用户ID
     */
    public String getUserIdByToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        return redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
    }

    /**
     * 刷新Token过期时间
     */
    public void refreshToken(String token) {
        if (token != null && !token.isEmpty()) {
            redisTemplate.expire(TOKEN_PREFIX + token, EXPIRE_SECONDS, TimeUnit.SECONDS);
        }
    }

    /**
     * 退出登录
     */
    public void logout(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        String userId = getUserIdByToken(token);
        redisTemplate.delete(TOKEN_PREFIX + token);
        if (userId != null) {
            redisTemplate.opsForSet().remove(USER_TOKEN_PREFIX + userId, token);
        }
    }
}