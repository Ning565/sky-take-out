package com.star.vo;

import lombok.Data;
import java.util.List;

@Data
public class ChatResponse {
    private String messageText; // AI回复文本
    private List<DishRecommendation> recommendations; // 推荐菜品列表
    private List<String> followUpQuestions; // 后续追问
    private String sessionId; // 会话ID
    private String conversationState; // 对话状态
} 