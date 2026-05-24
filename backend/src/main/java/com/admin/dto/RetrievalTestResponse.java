package com.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RetrievalTestResponse {
    private String originalQuestion;
    private String retrievalQuestion;
    private Boolean rewriteUsed;
    private List<RetrievalTestResult> results;
}
