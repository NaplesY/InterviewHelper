package com.recap.entity;

import com.recap.exception.BizException;
import com.recap.exception.ErrorCode;

/**
 * 录音状态枚举（对齐 tech-spec §4.3）。
 * DB 存 TINYINT（{@link #getDbValue()}），JSON 输出字符串（{@link #getJsonValue()}）。
 */
public enum RecordingStatus {

    TRANSFERING(0, "TRANSFERING"),
    TRANSCRIBED(1, "TRANSCRIBED"),
    SUMMARIZING(2, "SUMMARIZING"),
    DONE(3, "DONE"),
    FAILED(4, "FAILED");

    private final int dbValue;
    private final String jsonValue;

    RecordingStatus(int dbValue, String jsonValue) {
        this.dbValue = dbValue;
        this.jsonValue = jsonValue;
    }

    /**
     * DB TINYINT → 枚举；找不到抛 {@link BizException}(PARAM_INVALID)。
     */
    public static RecordingStatus fromDbValue(int dbValue) {
        for (RecordingStatus status : values()) {
            if (status.dbValue == dbValue) {
                return status;
            }
        }
        throw new BizException(ErrorCode.PARAM_INVALID, "非法状态值: " + dbValue);
    }

    public int getDbValue() {
        return dbValue;
    }

    public String getJsonValue() {
        return jsonValue;
    }
}
