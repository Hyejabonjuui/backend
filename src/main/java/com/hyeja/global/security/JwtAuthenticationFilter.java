package com.hyeja.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

// 요청마다 Authorization: Bearer <토큰> 헤더를 확인해, 유효한 토큰이면 로그인한 회원으로 표시합니다.
// 토큰이 없거나 잘못됐거나 로그아웃된 토큰이면 표시하지 않고 그냥 넘깁니다.
// 이후 인증이 필요한 API라면 SecurityConfig가 401(COMMON_002)로 막습니다.
// @Component로 등록하면 서블릿 필터로도 한 번 더 등록되므로, SecurityConfig에서 직접 생성합니다.
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final TokenBlacklist tokenBlacklist;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            authenticate(header.substring(BEARER_PREFIX.length()));
        }
        filterChain.doFilter(request, response);
    }

    // principal = 회원 ID(Long), credentials = 토큰 원문(로그아웃에서 사용)
    private void authenticate(String token) {
        Claims claims;
        try {
            claims = jwtProvider.parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            return;
        }
        if (tokenBlacklist.contains(token)) {
            return;
        }
        var authentication = new UsernamePasswordAuthenticationToken(
                Long.valueOf(claims.getSubject()), token,
                List.of(new SimpleGrantedAuthority("ROLE_" + claims.get("role", String.class))));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
