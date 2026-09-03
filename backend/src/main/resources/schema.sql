-- 面试复盘 App 后端建表 DDL（照抄 docs/tech-spec.md §4.2）
-- 用法：先建库 CREATE DATABASE IF NOT EXISTS recap; 再在 recap 库执行本文件。

CREATE TABLE recording (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(255) NOT NULL,           -- 默认取文件名
  file_path   VARCHAR(512) NOT NULL,           -- 本地磁盘路径
  file_size   BIGINT,
  duration_ms BIGINT,                          -- 时长（毫秒）
  format      VARCHAR(16),                     -- m4a/amr/wav/mp3...
  status      TINYINT NOT NULL DEFAULT 0,      -- 0=转写中 1=转写完成 2=总结中 3=已完成 4=失败
  transcript  LONGTEXT,                        -- 转写全文
  summary     TEXT,                            -- AI 总结
  tags        VARCHAR(255),                    -- 逗号分隔
  error_msg   VARCHAR(255),                    -- 失败原因
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE message (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  recording_id BIGINT NOT NULL,
  role         VARCHAR(16) NOT NULL,           -- user / assistant
  content      TEXT NOT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_recording (recording_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
