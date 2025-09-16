package com.star.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AiConversation {
    private Long id; // 对话记录ID
    private String sessionId; // 会话ID
    private Long userId; // 用户ID
    private String userMessage; // 用户输入的消息
    private String aiResponse; // AI回复的消息
    private String conversationContext; // 对话上下文数据（JSON）
    private String conversationState; // 对话状态
    private String modelInfo; // 使用的模型信息（JSON）
    private Integer responseTime; // 响应时间（毫秒）
    private String tokenUsage; // Token使用情况（JSON）
    private LocalDateTime createdTime; // 创建时间
    private LocalDateTime updatedTime; // 更新时间
} 