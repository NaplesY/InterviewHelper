package com.recap.mapper;

import com.recap.entity.Recording;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RecordingMapper 集成测试（真实 MySQL + 全上下文，验证 map-underscore-to-camel-case 生效）。
 */
@SpringBootTest
class RecordingMapperTest {

    @Autowired
    private RecordingMapper recordingMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.update("DELETE FROM recording");
    }

    private Recording buildRecording() {
        Recording r = new Recording();
        r.setTitle("字节一面");
        r.setFilePath("2026/09/abc.m4a");
        r.setFileSize(2048L);
        r.setDurationMs(3600000L);
        r.setFormat("m4a");
        r.setStatus(0);
        r.setTags("后端,八股");
        return r;
    }

    @Test
    void insertAndFindById_roundTripsAllFieldsWithCamelCaseMapping() {
        Recording r = buildRecording();

        int rows = recordingMapper.insert(r);
        assertThat(rows).isEqualTo(1);
        assertThat(r.getId()).isNotNull(); // 主键回填

        Recording loaded = recordingMapper.findById(r.getId());
        assertThat(loaded).isNotNull();
        assertThat(loaded.getId()).isEqualTo(r.getId());
        assertThat(loaded.getTitle()).isEqualTo("字节一面");
        assertThat(loaded.getFilePath()).isEqualTo("2026/09/abc.m4a"); // file_path -> filePath
        assertThat(loaded.getFileSize()).isEqualTo(2048L);             // file_size -> fileSize
        assertThat(loaded.getDurationMs()).isEqualTo(3600000L);        // duration_ms -> durationMs
        assertThat(loaded.getFormat()).isEqualTo("m4a");
        assertThat(loaded.getStatus()).isEqualTo(0);
        assertThat(loaded.getTags()).isEqualTo("后端,八股");
        assertThat(loaded.getErrorMsg()).isNull();                     // error_msg -> errorMsg
        assertThat(loaded.getCreatedAt()).isNotNull();                 // created_at -> createdAt（DB 默认值）
    }

    @Test
    void findAll_returnsRecordsOrderedByCreatedAtDesc() {
        Recording first = buildRecording();
        first.setTitle("first");
        recordingMapper.insert(first);

        Recording second = buildRecording();
        second.setTitle("second");
        recordingMapper.insert(second);

        List<Recording> all = recordingMapper.findAll();
        assertThat(all).hasSize(2);
        assertThat(all.get(0).getTitle()).isEqualTo("second");
        assertThat(all.get(1).getTitle()).isEqualTo("first");
    }

    @Test
    void updateStatus_persistsStatusAndErrorMsg() {
        Recording r = buildRecording();
        recordingMapper.insert(r);

        int rows = recordingMapper.updateStatus(r.getId(), 4, "转写失败");
        assertThat(rows).isEqualTo(1);

        Recording loaded = recordingMapper.findById(r.getId());
        assertThat(loaded.getStatus()).isEqualTo(4);
        assertThat(loaded.getErrorMsg()).isEqualTo("转写失败");
    }

    @Test
    void updateTranscript_persistsTranscript() {
        Recording r = buildRecording();
        recordingMapper.insert(r);

        recordingMapper.updateTranscript(r.getId(), "这是转写全文");

        assertThat(recordingMapper.findById(r.getId()).getTranscript()).isEqualTo("这是转写全文");
    }

    @Test
    void updateSummary_persistsSummary() {
        Recording r = buildRecording();
        recordingMapper.insert(r);

        recordingMapper.updateSummary(r.getId(), "这是总结");

        assertThat(recordingMapper.findById(r.getId()).getSummary()).isEqualTo("这是总结");
    }

    @Test
    void updateTitleAndTags_updatesOnlyProvidedFields() {
        Recording r = buildRecording();
        recordingMapper.insert(r);

        recordingMapper.updateTitleAndTags(r.getId(), "新标题", null);
        Recording loaded = recordingMapper.findById(r.getId());
        assertThat(loaded.getTitle()).isEqualTo("新标题");
        assertThat(loaded.getTags()).isEqualTo("后端,八股"); // tags 未传，保持不变

        recordingMapper.updateTitleAndTags(r.getId(), null, "前端,项目");
        loaded = recordingMapper.findById(r.getId());
        assertThat(loaded.getTitle()).isEqualTo("新标题");   // title 未传，保持不变
        assertThat(loaded.getTags()).isEqualTo("前端,项目");
    }

    @Test
    void deleteById_removesRecord() {
        Recording r = buildRecording();
        recordingMapper.insert(r);

        int rows = recordingMapper.deleteById(r.getId());
        assertThat(rows).isEqualTo(1);
        assertThat(recordingMapper.findById(r.getId())).isNull();
    }
}
