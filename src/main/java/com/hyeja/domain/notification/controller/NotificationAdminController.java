package com.hyeja.domain.notification.controller;

import com.hyeja.domain.notification.service.NotificationGenerationService;
import com.hyeja.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "알림 관리자", description = "개발·테스트용 알림 관리 API")
@RestController
@RequestMapping("/api/notification/admin")
@RequiredArgsConstructor
public class NotificationAdminController {

    private final NotificationGenerationService notificationGenerationService;
    private final Clock clock;

    @Operation(
            summary = "회원 마감 알림 생성",
            description = "서울 기준 오늘부터 7일 뒤 마감되는 관심 정책 알림을 지정한 회원에게만 생성합니다. "
                    + "이미 같은 회원·정책·마감일 알림이 있으면 중복 생성하지 않습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "알림 생성 성공. result는 새로 생성한 알림 개수입니다."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 회원 ID",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "회원을 찾을 수 없음 (MEMBER_001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping("/generate")
    public ApiResponse<Integer> generateNotifications(
            @Parameter(
                    name = "memberId",
                    description = "알림을 생성할 회원 ID",
                    in = ParameterIn.QUERY,
                    example = "1",
                    required = true
            )
            @RequestParam(name = "memberId")
            @Positive(message = "회원 ID는 양수여야 합니다.") Long memberId
    ) {
        int createdCount = notificationGenerationService.createDeadlineNotificationsForMember(
                LocalDate.now(clock),
                memberId
        );
        return ApiResponse.onSuccess(createdCount);
    }
}
