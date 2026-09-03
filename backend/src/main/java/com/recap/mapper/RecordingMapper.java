package com.recap.mapper;

import com.recap.entity.Recording;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RecordingMapper {

    @Insert("INSERT INTO recording (title, file_path, file_size, duration_ms, format, status, transcript, summary, tags, error_msg) " +
            "VALUES (#{title}, #{filePath}, #{fileSize}, #{durationMs}, #{format}, #{status}, #{transcript}, #{summary}, #{tags}, #{errorMsg})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Recording r);

    @Select("SELECT * FROM recording WHERE id = #{id}")
    Recording findById(@Param("id") Long id);

    @Select("SELECT * FROM recording ORDER BY created_at DESC, id DESC")
    List<Recording> findAll();

    @Update("<script>" +
            "UPDATE recording <set>" +
            "<if test='title != null'>title = #{title},</if>" +
            "<if test='tags != null'>tags = #{tags},</if>" +
            "</set> WHERE id = #{id}" +
            "</script>")
    int updateTitleAndTags(@Param("id") Long id, @Param("title") String title, @Param("tags") String tags);

    @Update("UPDATE recording SET status = #{status}, error_msg = #{errorMsg} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("errorMsg") String errorMsg);

    @Update("UPDATE recording SET transcript = #{transcript} WHERE id = #{id}")
    int updateTranscript(@Param("id") Long id, @Param("transcript") String transcript);

    @Update("UPDATE recording SET summary = #{summary} WHERE id = #{id}")
    int updateSummary(@Param("id") Long id, @Param("summary") String summary);

    @Delete("DELETE FROM recording WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
