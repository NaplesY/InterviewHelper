package com.recap.mapper;

import com.recap.entity.Message;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MessageMapper {

    @Insert("INSERT INTO message (recording_id, role, content) VALUES (#{recordingId}, #{role}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Message m);

    @Select("SELECT * FROM message WHERE recording_id = #{recordingId} ORDER BY created_at ASC, id ASC")
    List<Message> findByRecordingId(@Param("recordingId") Long recordingId);

    @Delete("DELETE FROM message WHERE recording_id = #{recordingId}")
    int deleteByRecordingId(@Param("recordingId") Long recordingId);
}
