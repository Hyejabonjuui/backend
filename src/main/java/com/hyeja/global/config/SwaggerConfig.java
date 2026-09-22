package com.hyeja.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI hyejaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("혜자 API")
                        .description("내 조건에 딱 맞는 청년 주거 혜택, 혜자")
                        .version("v1"));
    }
}
