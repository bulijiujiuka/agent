package com.admin.mapper;

import com.admin.dto.KnowledgeSearchHit;
import com.admin.entity.KbChunk;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KbChunkMapper {

    @Select("SELECT * FROM kb_chunk WHERE document_id = #{documentId} ORDER BY chunk_index ASC")
    List<KbChunk> findByDocumentId(Long documentId);

    @Select("SELECT * FROM kb_chunk WHERE id = #{id}")
    KbChunk findById(Long id);

    @Insert("INSERT INTO kb_chunk (document_id, chunk_index, chunk_text, char_count, vector_status ,section_title ,title_path) " +
            "VALUES (#{documentId}, #{chunkIndex}, #{chunkText}, #{charCount}, #{vectorStatus} ,#{sectionTitle}, #{titlePath})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KbChunk chunk);

    @Delete("DELETE FROM kb_chunk WHERE document_id = #{documentId}")
    int deleteByDocumentId(Long documentId);

    @Select("SELECT * FROM kb_chunk WHERE document_id = #{documentId} AND vector_status <> 'COMPLETED' ORDER BY chunk_index ASC")
    List<KbChunk> findNotCompletedByDocumentId(Long documentId);

    @Update("UPDATE kb_chunk SET vector_status = #{status} WHERE id = #{id}")
    int updateVectorStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE kb_chunk SET vector_status = 'PENDING' WHERE document_id = #{documentId}")
    int resetVectorStatusByDocumentId(Long documentId);

    @Select("""
            SELECT c.id AS chunkId,
                   c.document_id AS documentId,
                   d.document_name AS documentName,
                   c.chunk_index AS chunkIndex,
                   c.chunk_text AS text,
                   d.category AS category,
                   d.version As version,
                   c.section_title AS sectionTitle,
                   c.title_path AS titlePath,
                   MATCH(c.chunk_text) AGAINST(#{query} IN NATURAL LANGUAGE MODE) AS bm25Score
            FROM kb_chunk c
            JOIN kb_document d ON c.document_id = d.id
            WHERE d.enabled = 1
              AND (#{documentId} IS NULL OR c.document_id = #{documentId})
              AND MATCH(c.chunk_text) AGAINST(#{query} IN NATURAL LANGUAGE MODE)
            ORDER BY bm25Score DESC
            LIMIT #{limit}
            """)
    List<KnowledgeSearchHit> fulltextSearch(@Param("query") String query,
                                            @Param("documentId") Long documentId,
                                            @Param("limit") int limit);

    @Select("""
            SELECT c.id AS chunkId,
                   c.document_id AS documentId,
                   d.document_name AS documentName,
                   c.chunk_index AS chunkIndex,
                   c.chunk_text AS text,
                   d.category AS category,
                   d.version As version,
                   c.section_title AS sectionTitle,
                   c.title_path AS titlePath,
                   1.0 AS bm25Score
            FROM kb_chunk c
            JOIN kb_document d ON c.document_id = d.id
            WHERE d.enabled = 1
              AND (#{documentId} IS NULL OR c.document_id = #{documentId})
              AND c.chunk_text LIKE CONCAT('%', #{query}, '%')
            ORDER BY c.id DESC
            LIMIT #{limit}
            """)
    List<KnowledgeSearchHit> likeSearch(@Param("query") String query,
                                        @Param("documentId") Long documentId,
                                        @Param("limit") int limit);

    @Select("""
            select * from kb_chunk
            where document_id = #{documentId}
              and chunk_index between #{startIndex} and #{endIndex}
            order by chunk_index asc
            """)
    List<KbChunk> findWindow(@Param("documentId") Long documentId,
                             @Param("startIndex") int startIndex,
                             @Param("endIndex") int endIndex);
}
