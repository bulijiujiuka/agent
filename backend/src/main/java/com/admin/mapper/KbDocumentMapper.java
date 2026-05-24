package com.admin.mapper;

import com.admin.entity.KbDocument;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KbDocumentMapper {

    @Select("SELECT * FROM kb_document ORDER BY create_time DESC")
    List<KbDocument> findAll();

    @Select("SELECT * FROM kb_document WHERE enabled = 1 ORDER BY create_time DESC")
    List<KbDocument> findAllEnabled();

    @Select("SELECT * FROM kb_document WHERE id = #{id}")
    KbDocument findById(Long id);

    @Select("<script>" +
            "SELECT COUNT(*) FROM kb_document" +
            "<where>" +
            "  <if test='documentName != null and documentName != \"\"'> AND document_name LIKE CONCAT('%',#{documentName},'%')</if>" +
            "  <if test='category != null and category != \"\"'> AND category = #{category}</if>" +
            "  <if test='parseStatus != null and parseStatus != \"\"'> AND parse_status = #{parseStatus}</if>" +
            "</where>" +
            "</script>")
    long countByCondition(@Param("documentName") String documentName,
                          @Param("category") String category,
                          @Param("parseStatus") String parseStatus);

    @Select("<script>" +
            "SELECT * FROM kb_document" +
            "<where>" +
            "  <if test='documentName != null and documentName != \"\"'> AND document_name LIKE CONCAT('%',#{documentName},'%')</if>" +
            "  <if test='category != null and category != \"\"'> AND category = #{category}</if>" +
            "  <if test='parseStatus != null and parseStatus != \"\"'> AND parse_status = #{parseStatus}</if>" +
            "</where>" +
            " ORDER BY create_time DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    List<KbDocument> findByPage(@Param("documentName") String documentName,
                                @Param("category") String category,
                                @Param("parseStatus") String parseStatus,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    @Insert("INSERT INTO kb_document (file_id, document_name, source_type, category, tags, content_type, " +
            "parse_status, embedding_status, chunk_count, enabled, version, summary, uploaded_by) " +
            "VALUES (#{fileId}, #{documentName}, #{sourceType}, #{category}, #{tags}, #{contentType}, " +
            "#{parseStatus}, #{embeddingStatus}, #{chunkCount}, #{enabled}, #{version}, #{summary}, #{uploadedBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KbDocument document);

    @Update("UPDATE kb_document SET document_name = #{documentName}, category = #{category}, tags = #{tags}, " +
            "parse_status = #{parseStatus}, embedding_status = #{embeddingStatus}, " +
            "chunk_count = #{chunkCount}, summary = #{summary} WHERE id = #{id}")
    int update(KbDocument document);

    @Update("UPDATE kb_document SET enabled = #{enabled} WHERE id = #{id}")
    int updateEnabled(@Param("id") Long id, @Param("enabled") boolean enabled);

    @Update("UPDATE kb_document SET version = version + 1 WHERE id = #{id}")
    int incrementVersion(Long id);

    @Delete("DELETE FROM kb_document WHERE id = #{id}")
    int deleteById(Long id);

    @Update("UPDATE kb_document SET embedding_status = #{status} WHERE id = #{id}")
    int updateEmbeddingStatus(@Param("id") Long id, @Param("status") String status);
}
