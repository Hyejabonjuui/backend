package com.hyeja.domain.notification.controller;

import com.hyeja.domain.notification.dto.NotificationResponseDTO.NotificationItemDTO;
import com.hyeja.domain.notification.dto.NotificationResponseDTO.NotificationListDTO;
import com.hyeja.domain.notification.service.NotificationService;
import com.hyeja.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "알림", description = "회원 알림 API")
@Validated
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "알림 목록 조회",
            description = "회원 ID에 해당하는 삭제되지 않은 알림을 최신순으로 8개씩 조회합니다. "
                    + "더보기 요청 시 page 값을 1씩 증가시킵니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "알림 목록 조회 성공"
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
    @GetMapping("/{memberId}")
    public ApiResponse<NotificationListDTO> getNotifications(
            @Parameter(description = "조회할 회원 ID", example = "1", required = true) // 추후 memberId는 없앨 예정
            @PathVariable("memberId") @Positive(message = "회원 ID는 양수여야 합니다.") Long memberId,
            @Parameter(name = "page", description = "페이지 번호(0부터 시작)", in = ParameterIn.QUERY, example = "0")
            @RequestParam(name = "page", defaultValue = "0")
            @PositiveOrZero(message = "페이지 번호는 0 이상이어야 합니다.") int page,
            @Parameter(name = "size", description = "페이지당 알림 개수", in = ParameterIn.QUERY, example = "8")
            @RequestParam(name = "size", defaultValue = "8")
            @Positive(message = "페이지 크기는 양수여야 합니다.") int size
    ) {
        NotificationListDTO result = notificationService.getNotifications(memberId, page, size);
        return ApiResponse.onSuccess(result);
    }

    @Operation(
            summary = "알림 읽음 처리",
            description = "회원 본인의 삭제되지 않은 알림을 읽음 상태로 변경합니다. "
                    + "이미 읽은 알림에 다시 요청해도 읽음 상태를 유지합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "알림 읽음 처리 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 회원 ID 또는 알림 ID",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "회원을 찾을 수 없음 (MEMBER_001) 또는 알림을 찾을 수 없음 (NOTIFICATION_001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationItemDTO> markNotificationAsRead(
            @Parameter(description = "읽음 처리할 알림 ID", example = "1", required = true)
            @PathVariable("notificationId")
            @Positive(message = "알림 ID는 양수여야 합니다.") Long notificationId,
            @Parameter(
                    name = "memberId",
                    description = "알림 소유 회원 ID",
                    in = ParameterIn.QUERY,
                    example = "1",
                    required = true
            ) // 추후 인증 도입 시 제거 예정
            @RequestParam("memberId")
            @Positive(message = "회원 ID는 양수여야 합니다.") Long memberId
    ) {
        NotificationItemDTO result = notificationService.markAsRead(memberId, notificationId);
        return ApiResponse.onSuccess(result);
    }

    @Operation(
            summary = "알림 삭제",
            description = "회원 본인의 삭제되지 않은 알림을 데이터베이스에서 영구 삭제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "알림 삭제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 회원 ID 또는 알림 ID",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "회원을 찾을 수 없음 (MEMBER_001) 또는 알림을 찾을 수 없음 (NOTIFICATION_001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @DeleteMapping("/{notificationId}")
    public ApiResponse<Void> deleteNotification(
            @Parameter(description = "삭제할 알림 ID", example = "1", required = true)
            @PathVariable("notificationId")
            @Positive(message = "알림 ID는 양수여야 합니다.") Long notificationId,
            @Parameter(
                    name = "memberId",
                    description = "알림 소유 회원 ID",
                    in = ParameterIn.QUERY,
                    example = "1",
                    required = true
            ) // 추후 인증 도입 시 제거 예정
            @RequestParam("memberId")
            @Positive(message = "회원 ID는 양수여야 합니다.") Long memberId
    ) {
        notificationService.deleteNotification(memberId, notificationId);
        return ApiResponse.onSuccess(null);
    }
}
