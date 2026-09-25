package com.hyeja.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// 비밀번호 암호화(BCrypt) 빈입니다. 회원가입에서 암호화하고, 이후 로그인에서도 같은 빈으로 비교합니다.
// spring-security-crypto만 쓰므로 Spring Security 필터(로그인 화면·인증 차단)는 켜지지 않습니다.
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
