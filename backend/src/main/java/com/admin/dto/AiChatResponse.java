package com.admin.dto;

import lombok.Data;

@Data
public class AiChatResponse {

    private String answer;

    private String referenceContent;

    public AiChatResponse(String answer, String referenceContent) {
        this.answer = answer;
        this.referenceContent = referenceContent;
    }
}
