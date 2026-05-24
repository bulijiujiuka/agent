package com.admin.dto;

import lombok.Data;

@Data
public class KnowledgeSearchHit {
    private String chunkId;
    private String documentId;
    private String documentName;
    private String chunkIndex;
    private String text;
    private Double vectorScore;
    private Double bm25Score;
    private Double finalScore;
    private String source;

    private String category;
    private String sectionTitle;
    private String titlePath;
    private Integer version;
}
