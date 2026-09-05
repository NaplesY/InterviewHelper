package com.recap.exception;

import lombok.Getter;

/**
 * 统一错误响应体 {@code { code, message }}（对齐 tech-spec §4.4）。
 */
@Getter
public class ErrorResponse {

    private final int code;
    private final String message;

    public ErrorResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getCode(), message);
    }
}
