package com.admin.dto;

import lombok.Data;

@Data
public class RetrievalTestRequest {
    private String question;
    private Long documentId;
    private Integer topK = 5;
    private Double minScore = 0.3;

    // 是否启用查询重写
    private Boolean rewrite = false;

    // 用哪个会话的上下文来做查询重写
    private Long conversationId;

    // HYBRID / VECTOR, defaults to the production hybrid retrieval path.
    private String retrievalMode = "HYBRID";

    // Production retrieval expands neighbor chunks; evaluation can disable it.
    private Boolean expandNeighbors = true;

    // Production retrieval uses cache; evaluation can disable it for fair latency.
    private Boolean useCache = true;
}
