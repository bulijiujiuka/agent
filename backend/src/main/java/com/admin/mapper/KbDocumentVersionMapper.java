package com.admin.mapper;

import com.admin.entity.KbDocumentVersion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KbDocumentVersionMapper {

    @Select("SELECT * FROM kb_document_version WHERE document_id = #{documentId} ORDER BY version DESC")
    List<KbDocumentVersion> findByDocumentId(Long documentId);

    @Select("SELECT * FROM kb_document_version WHERE document_id = #{documentId} AND version = #{version} ORDER BY id DESC LIMIT 1")
    KbDocumentVersion findByDocumentIdAndVersion(@Param("documentId") Long documentId, @Param("version") int version);

    @Insert("INSERT INTO kb_document_version (document_id, version, document_name, category, tags, summary, content_snapshot, created_by) " +
            "VALUES (#{documentId}, #{version}, #{documentName}, #{category}, #{tags}, #{summary}, #{contentSnapshot}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KbDocumentVersion version);

    @Delete("DELETE FROM kb_document_version WHERE document_id = #{documentId}")
    int deleteByDocumentId(Long documentId);
}
