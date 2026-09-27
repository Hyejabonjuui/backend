package com.hyeja;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// 로그인은 JWT로 직접 처리하므로, 스프링이 기본 계정을 만들며 띄우는 "generated security password" 경고를 끕니다.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)

public class HyejaApplication {

	public static void main(String[] args) {
		SpringApplication.run(HyejaApplication.class, args);
	}

}
