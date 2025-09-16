package com.star.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String sessionId; // 会话ID
    private String message;   // 用户输入消息
    private Long userId;      // 用户ID（可选）
} 