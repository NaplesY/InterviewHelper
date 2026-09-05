package com.recap.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ErrorResponse 工厂方法契约测试。
 */
class ErrorResponseTest {

    @Test
    void of_errorCode_usesCodeAndDefaultMessage() {
        ErrorResponse resp = ErrorResponse.of(ErrorCode.RECORDING_NOT_FOUND);

        assertThat(resp.getCode()).isEqualTo(1001);
        assertThat(resp.getMessage()).isEqualTo("记录不存在");
    }

    @Test
    void of_errorCodeAndMessage_overridesMessage() {
        ErrorResponse resp = ErrorResponse.of(ErrorCode.LLM_FAILED, "模型超时");

        assertThat(resp.getCode()).isEqualTo(1005);
        assertThat(resp.getMessage()).isEqualTo("模型超时");
    }
}
