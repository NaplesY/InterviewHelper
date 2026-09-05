package com.recap.exception;

/**
 * 统一错误码（对齐 tech-spec §4.4）。
 */
public enum ErrorCode {

    SUCCESS(0, 200, "成功"),
    RECORDING_NOT_FOUND(1001, 404, "记录不存在"),
    FILE_EMPTY(1002, 400, "文件为空"),
    FORMAT_NOT_SUPPORTED(1003, 400, "音频格式不支持 / 转码失败"),
    ASR_FAILED(1004, 502, "ASR 转写失败"),
    LLM_FAILED(1005, 502, "LLM 调用失败"),
    PARAM_INVALID(1006, 400, "参数校验失败");

    private final int code;
    private final int httpStatus;
    private final String message;

    ErrorCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
