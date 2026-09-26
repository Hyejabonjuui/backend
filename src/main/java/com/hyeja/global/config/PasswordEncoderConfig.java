package com.hyeja.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// 비밀번호 암호화(BCrypt) 빈입니다. 회원가입에서 암호화하고, 로그인에서 같은 빈으로 비교합니다.
// SecurityConfig에서 PasswordEncoder 빈을 또 만들면 충돌하니 여기 하나만 둡니다.
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
