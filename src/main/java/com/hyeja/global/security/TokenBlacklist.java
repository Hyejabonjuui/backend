package com.hyeja.global.security;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

// 로그아웃한 토큰 목록(Redis). JWT는 한번 발급하면 서버가 되돌릴 수 없어서,
// 로그아웃한 토큰을 여기에 적어 두고 인증 필터가 거부합니다.
// 토큰이 원래 만료될 때까지만 보관하고, 그 뒤에는 Redis가 자동으로 지웁니다(어차피 만료라 쓸 수 없음).
@Component
@RequiredArgsConstructor
public class TokenBlacklist {

    private static final String KEY_PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;

    public void add(String token, Duration remaining) {
        if (!remaining.isNegative() && !remaining.isZero()) {
            redisTemplate.opsForValue().set(KEY_PREFIX + token, "logout", remaining);
        }
    }

    public boolean contains(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + token));
    }
}
