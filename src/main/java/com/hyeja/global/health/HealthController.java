package com.hyeja.global.health;

import com.hyeja.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "서버 상태 확인")
@RestController
public class HealthController {

    @Operation(summary = "헬스체크", description = "서버가 실행 중이면 UP을 반환합니다.")
    @GetMapping("/api/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.onSuccess(Map.of("status", "UP"));
    }
}
