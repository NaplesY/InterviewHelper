package com.recap.entity;

import com.recap.exception.BizException;
import com.recap.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RecordingStatus 枚举契约测试（对齐 tech-spec §4.3：DB int ↔ JSON str 互转）。
 */
class RecordingStatusTest {

    @Test
    void fromDbValue_mapsAllFiveValues() {
        assertThat(RecordingStatus.fromDbValue(0)).isEqualTo(RecordingStatus.TRANSFERING);
        assertThat(RecordingStatus.fromDbValue(1)).isEqualTo(RecordingStatus.TRANSCRIBED);
        assertThat(RecordingStatus.fromDbValue(2)).isEqualTo(RecordingStatus.SUMMARIZING);
        assertThat(RecordingStatus.fromDbValue(3)).isEqualTo(RecordingStatus.DONE);
        assertThat(RecordingStatus.fromDbValue(4)).isEqualTo(RecordingStatus.FAILED);
    }

    @Test
    void fromDbValue_unknownValue_throwsBizExceptionParamInvalid() {
        assertThatThrownBy(() -> RecordingStatus.fromDbValue(99))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    BizException be = (BizException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.PARAM_INVALID);
                });
    }

    @Test
    void getDbValueAndJsonValue_matchSpec() {
        assertThat(RecordingStatus.TRANSFERING.getDbValue()).isEqualTo(0);
        assertThat(RecordingStatus.TRANSFERING.getJsonValue()).isEqualTo("TRANSFERING");
        assertThat(RecordingStatus.FAILED.getDbValue()).isEqualTo(4);
        assertThat(RecordingStatus.FAILED.getJsonValue()).isEqualTo("FAILED");
    }
}
