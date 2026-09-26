package com.hyeja.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
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
                        .version("v1"))
                // Swagger 화면 오른쪽 위 Authorize 버튼에 로그인 토큰을 넣으면 인증이 필요한 API를 호출할 수 있습니다.
                // 토큰이 필요한 API에는 @SecurityRequirement(name = "bearerAuth")를 붙입니다.
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
