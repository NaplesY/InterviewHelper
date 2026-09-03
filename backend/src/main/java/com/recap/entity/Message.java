package com.recap.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对应 message 表。role 取 "user" / "assistant"。
 */
@Data
public class Message {

    private Long id;
    private Long recordingId;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
