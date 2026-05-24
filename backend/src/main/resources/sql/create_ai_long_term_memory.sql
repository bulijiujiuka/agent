CREATE TABLE IF NOT EXISTS `ai_long_term_memory` (
                                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '长期记忆ID',

                                                     `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID，支持用户级长期记忆',
                                                     `conversation_id` bigint NULL DEFAULT NULL COMMENT '来源会话ID',
                                                     `message_id` bigint NULL DEFAULT NULL COMMENT '来源消息ID',
                                                     `business_type` varchar(50) NOT NULL DEFAULT 'QA' COMMENT '业务场景：QA / TICKET / KNOWLEDGE / SYSTEM',

    `memory_type` varchar(50) NOT NULL COMMENT '记忆类型：USER_PREFERENCE / BUSINESS_FACT / TICKET_FACT / PROBLEM_SOLUTION / QA_EXPERIENCE',
    `memory_text` text NOT NULL COMMENT '长期记忆正文，用于向量化和召回注入',
    `memory_summary` varchar(1000) NULL DEFAULT NULL COMMENT '较短摘要，用于列表展示或快速预览',

    `source_type` varchar(50) NOT NULL COMMENT '来源类型：CHAT / TICKET / FEEDBACK / MANUAL / SYSTEM',
    `source_ref_id` bigint NULL DEFAULT NULL COMMENT '来源业务ID，例如工单ID、消息ID等',

    `importance_score` double NOT NULL DEFAULT 0 COMMENT '重要性评分，0-1',
    `confidence_score` double NOT NULL DEFAULT 0 COMMENT '置信度评分，0-1',
    `access_count` int NOT NULL DEFAULT 0 COMMENT '被召回次数',

    `embedding_status` varchar(30) NOT NULL DEFAULT 'PENDING' COMMENT '向量化状态：PENDING / PROCESSING / COMPLETED / FAILED',
    `vector_id` varchar(100) NULL DEFAULT NULL COMMENT '向量库中的ID，便于删除或更新向量',
    `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0禁用',

    `last_access_time` timestamp NULL DEFAULT NULL COMMENT '最近一次被召回时间',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),

    INDEX `idx_user_type` (`user_id`, `memory_type`),
    INDEX `idx_conversation_id` (`conversation_id`),
    INDEX `idx_message_id` (`message_id`),
    INDEX `idx_source` (`source_type`, `source_ref_id`),
    INDEX `idx_business_type` (`business_type`),
    INDEX `idx_embedding_status` (`embedding_status`),
    INDEX `idx_importance_score` (`importance_score`),
    INDEX `idx_enabled_create_time` (`enabled`, `create_time`)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_general_ci
    COMMENT='AI长期记忆表';