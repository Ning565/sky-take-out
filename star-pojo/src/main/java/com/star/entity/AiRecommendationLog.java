package com.star.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AiRecommendationLog {
    private Long id; // 推荐记录ID
    private String sessionId; // 会话ID
    private Long userId; // 用户ID
    private String userRequirements; // 用户需求（JSON）
    private String recommendedDishes; // 推荐菜品列表（JSON）
    private String recommendationAlgorithm; // 推荐算法类型
    private Double recommendationScore; // 推荐置信度
    private Boolean ragEnabled; // 是否使用RAG
    private String ragContext; // RAG上下文信息
    private String agentChain; // Agent调用链信息
    private Integer userFeedback; // 用户反馈
    private String feedbackComment; // 用户反馈评论
    private String clickedDishes; // 用户点击的菜品列表（JSON）
    private String orderedDishes; // 用户下单菜品列表（JSON）
    private BigDecimal totalPrice; // 推荐菜品总价格
    private BigDecimal actualOrderPrice; // 实际下单金额
    private LocalDateTime createdTime; // 创建时间
    private LocalDateTime updatedTime; // 更新时间
} 