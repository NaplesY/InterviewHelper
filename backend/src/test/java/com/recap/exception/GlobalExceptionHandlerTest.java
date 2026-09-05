package com.recap.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GlobalExceptionHandler 契约测试：BizException → 对应 httpStatus + 错误体；未知异常 → 500。
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @RestController
    static class TestController {
        @GetMapping("/not-found")
        void notFound() {
            throw new BizException(ErrorCode.RECORDING_NOT_FOUND);
        }

        @GetMapping("/bad-request")
        void badRequest() {
            throw new BizException(ErrorCode.PARAM_INVALID, "title 不能为空");
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new RuntimeException("boom");
        }
    }

    @Test
    void bizException_returnsHttpStatusAndErrorBody() throws Exception {
        mockMvc.perform(get("/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value("记录不存在"));
    }

    @Test
    void bizExceptionWithDetail_usesDetailMessage() throws Exception {
        mockMvc.perform(get("/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1006))
                .andExpect(jsonPath("$.message").value("title 不能为空"));
    }

    @Test
    void genericException_returns500InternalServerError() throws Exception {
        mockMvc.perform(get("/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("服务器内部错误"));
    }
}
