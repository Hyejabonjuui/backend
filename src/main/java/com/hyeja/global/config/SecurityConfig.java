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
                // 기본은 로그인 필수입니다. 로그인 없이 쓰는 API만 아래에 허용합니다.
                // 새 API를 만들면 자동으로 로그인 필수가 되므로, 비로그인용이면 여기에 추가해 주세요.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/members",
                                "/api/members/login",
                                "/api/members/email-verifications",
                                "/api/members/email-verifications/confirmation").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/members/find-email",
                                "/api/regions",
                                "/api/policies/housing",
                                "/api/policies/card-news/guest",
                                "/api/health").permitAll()
                        // Swagger 화면, 그리고 예외 발생 시 Spring이 내부적으로 넘기는 /error
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                        .anyRequest().authenticated())
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

    // 프론트(React Vite 개발 서버, 5173 포트)에서 오는 요청을 허용합니다.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
