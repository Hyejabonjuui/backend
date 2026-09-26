package com.hyeja.global.config;

import com.hyeja.global.apiPayload.status.ErrorStatus;
import com.hyeja.global.security.JwtAuthenticationFilter;
import com.hyeja.global.security.JwtProvider;
import com.hyeja.global.security.TokenBlacklist;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// Spring Security 설정입니다. 비밀번호 암호화 빈은 PasswordEncoderConfig에 있으니 여기서 다시 만들지 않습니다.
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final TokenBlacklist tokenBlacklist;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                // 세션·쿠키 대신 헤더의 토큰으로 인증하므로 CSRF·세션·기본 로그인 화면을 끕니다.
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/members/logout").authenticated()
                        // TODO: memberId 쿼리 파라미터 → 토큰 전환 이슈에서 로그인이 필요한 API를 authenticated()로 바꿉니다.
                        //       지금은 기존 API·프론트가 memberId로 동작하므로 모두 허용합니다.
                        .anyRequest().permitAll())
                // 인증이 필요한 API에 토큰이 없거나 잘못됐으면 공통 응답 형식의 401을 내려줍니다.
                .exceptionHandling(exception -> exception.authenticationEntryPoint((request, response, e) -> {
                    ErrorStatus error = ErrorStatus.UNAUTHORIZED;
                    response.setStatus(error.getHttpStatus().value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"isSuccess\":false,\"code\":\"%s\",\"message\":\"%s\",\"result\":null}"
                            .formatted(error.getCode(), error.getMessage()));
                }))
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, tokenBlacklist),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // 프론트(React 개발 서버)에서 오는 요청을 허용합니다.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
