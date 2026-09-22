package com.hyeja.global.apiPayload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hyeja.global.apiPayload.code.BaseCode;
import com.hyeja.global.apiPayload.dto.ReasonDTO;
import com.hyeja.global.apiPayload.status.SuccessStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class ApiResponse<T> {
    private final Boolean isSuccess;
    private final String code;
    private final String message;
    private final T result;

    public static <T> ApiResponse<T> onSuccess(T result) {
        return of(SuccessStatus.OK, result);
    }

    // HTTP 상태는 컨트롤러의 ResponseEntity에서 지정합니다.
    public static <T> ApiResponse<T> of(BaseCode status, T result) {
        ReasonDTO reason = status.getReasonDTO();
        return new ApiResponse<>(reason.getIsSuccess(), reason.getCode(), reason.getMessage(), result);
    }

    public static <T> ApiResponse<T> onFailure(String code, String message, T result) {
        return new ApiResponse<>(false, code, message, result);
    }
}
