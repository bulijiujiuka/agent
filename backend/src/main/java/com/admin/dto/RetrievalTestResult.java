package com.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RetrievalTestResult {
    private String documentId;
    private String documentName;
    private String chunkId;
    private String chunkIndex;
    private Double score;
    private Double vectorScore;
    private Double bm25Score;
    private String source;
    private String text;

    public RetrievalTestResult(String documentId, String documentName, String chunkId,
                               String chunkIndex, Double score, String text) {
        this.documentId = documentId;
        this.documentName = documentName;
        this.chunkId = chunkId;
        this.chunkIndex = chunkIndex;
        this.score = score;
        this.text = text;
    }
}
