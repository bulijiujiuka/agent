package com.admin.dto;

import lombok.Data;

@Data
public class AiChatRequest {

    private Long conversationId;

    private String question;

    private String modelKey;

    private Long documentId;
}
