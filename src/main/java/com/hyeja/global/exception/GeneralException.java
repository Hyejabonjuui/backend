package com.hyeja.global.exception;

import com.hyeja.global.apiPayload.code.BaseErrorCode;
import com.hyeja.global.apiPayload.dto.ReasonDTO;
import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException {
    private final BaseErrorCode code;

    public GeneralException(BaseErrorCode code) {
        super(code.getMessage());
        this.code = code;
    }

    public ReasonDTO getErrorReason() {
        return code.getErrorReasonDTO();
    }
}
