package com.recap.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ErrorCode 枚举契约测试（对齐 tech-spec §4.4 错误码表）。
 */
class ErrorCodeTest {

    @Test
    void errorCodes_haveExpectedCodeHttpStatusAndMessage() {
        assertErrorCode(ErrorCode.SUCCESS, 0, 200, "成功");
        assertErrorCode(ErrorCode.RECORDING_NOT_FOUND, 1001, 404, "记录不存在");
        assertErrorCode(ErrorCode.FILE_EMPTY, 1002, 400, "文件为空");
        assertErrorCode(ErrorCode.FORMAT_NOT_SUPPORTED, 1003, 400, "音频格式不支持 / 转码失败");
        assertErrorCode(ErrorCode.ASR_FAILED, 1004, 502, "ASR 转写失败");
        assertErrorCode(ErrorCode.LLM_FAILED, 1005, 502, "LLM 调用失败");
        assertErrorCode(ErrorCode.PARAM_INVALID, 1006, 400, "参数校验失败");
    }

    private void assertErrorCode(ErrorCode code, int expectedCode, int expectedHttpStatus, String expectedMessage) {
        assertThat(code.getCode()).isEqualTo(expectedCode);
        assertThat(code.getHttpStatus()).isEqualTo(expectedHttpStatus);
        assertThat(code.getMessage()).isEqualTo(expectedMessage);
    }
}
