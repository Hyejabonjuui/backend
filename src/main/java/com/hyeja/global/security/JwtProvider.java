package com.hyeja.global.security;

import com.hyeja.domain.member.entity.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 로그인 토큰(JWT)을 만들고 검증합니다.
// 토큰 안에는 회원 ID(subject)와 권한(role)만 넣습니다. 이메일·닉네임 같은 개인정보는 넣지 않습니다.
@Component
public class JwtProvider {

    // 명세상 refresh token은 없어서, 만료되면 다시 로그인합니다.
    private static final Duration ACCESS_TOKEN_EXPIRY = Duration.ofMinutes(300);

    private final SecretKey key;

    // jwt.secret은 .env의 JWT_SECRET 값입니다. HS256 서명에는 32바이트 이상이 필요합니다(짧으면 서버가 뜨지 않음).
    public JwtProvider(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Member member) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(member.getMemberId()))
                .claim("role", member.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRY.toMillis()))
                .signWith(key)
                .compact();
    }

    // 서명이 틀리거나(위조) 만료됐거나 형식이 잘못되면 JwtException을 던집니다.
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
