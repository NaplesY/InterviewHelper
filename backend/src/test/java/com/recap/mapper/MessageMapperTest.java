package com.recap.mapper;

import com.recap.entity.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MessageMapper 集成测试。
 */
@SpringBootTest
class MessageMapperTest {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.update("DELETE FROM message");
    }

    private Message buildMessage(Long recordingId, String role, String content) {
        Message m = new Message();
        m.setRecordingId(recordingId);
        m.setRole(role);
        m.setContent(content);
        return m;
    }

    @Test
    void insertAndFindByRecordingId_roundTripsWithOrderAsc() {
        messageMapper.insert(buildMessage(100L, "user", "第一问"));
        Message m2 = buildMessage(100L, "assistant", "第一答");
        messageMapper.insert(m2);
        assertThat(m2.getId()).isNotNull(); // 主键回填

        List<Message> messages = messageMapper.findByRecordingId(100L);
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getRole()).isEqualTo("user");
        assertThat(messages.get(0).getContent()).isEqualTo("第一问");
        assertThat(messages.get(1).getRole()).isEqualTo("assistant");
        assertThat(messages.get(1).getContent()).isEqualTo("第一答");
        assertThat(messages.get(1).getCreatedAt()).isNotNull(); // created_at -> createdAt
    }

    @Test
    void findByRecordingId_returnsEmptyForNoMessages() {
        assertThat(messageMapper.findByRecordingId(999L)).isEmpty();
    }

    @Test
    void deleteByRecordingId_removesAllMessagesOfRecording() {
        for (int i = 0; i < 3; i++) {
            messageMapper.insert(buildMessage(200L, "user", "内容" + i));
        }

        int rows = messageMapper.deleteByRecordingId(200L);
        assertThat(rows).isEqualTo(3);
        assertThat(messageMapper.findByRecordingId(200L)).isEmpty();
    }
}
