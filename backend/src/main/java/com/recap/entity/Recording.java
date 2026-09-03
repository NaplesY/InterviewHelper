package com.recap.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对应 recording 表。status 存 RecordingStatus.dbValue（0-4）。
 */
@Data
public class Recording {

    private Long id;
    private String title;
    private String filePath;
    private Long fileSize;
    private Long durationMs;
    private String format;
    private Integer status;
    private String transcript;
    private String summary;
    private String tags;
    private String errorMsg;
    private LocalDateTime createdAt;
}
