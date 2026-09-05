package com.recap.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BizException 携带 ErrorCode 与消息的契约测试。
 */
class BizExceptionTest {

    @Test
    void constructor_carriesErrorCodeAndDefaultMessage() {
        BizException ex = new BizException(ErrorCode.RECORDING_NOT_FOUND);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RECORDING_NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("记录不存在");
    }

    @Test
    void constructorWithDetail_carriesErrorCodeAndDetailMessage() {
        BizException ex = new BizException(ErrorCode.PARAM_INVALID, "title 不能为空");

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PARAM_INVALID);
        assertThat(ex.getMessage()).isEqualTo("title 不能为空");
    }
}
