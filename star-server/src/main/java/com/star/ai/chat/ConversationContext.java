package com.star.ai.chat;

import com.star.ai.agent.base.ConversationMessage;
import com.star.pojo.dto.UserRequirement;
import com.star.pojo.vo.DishRecommendation;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 对话上下文
 * 封装对话过程中的上下文信息，包括用户状态、对话历史、推荐结果等。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationContext {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 对话历史记录
     */
    private List<ConversationMessage> conversationHistory;
    
    /**
     * 用户当前状态
     */
    private ConversationState currentState;
    
    /**
     * 用户需求信息
     */
    private UserRequirement userRequirement;
    
    /**
     * 当前推荐结果
     */
    private List<DishRecommendation> currentRecommendations;
    
    /**
     * 用户偏好学习数据
     */
    private Map<String, Object> userPreferences;
    
    /**
     * 对话开始时间
     */
    private LocalDateTime conversationStartTime;
    
    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;
    
    /**
     * 对话轮次
     */
    private Integer conversationRound;
    
    /**
     * 上下文标签（用于个性化）
     */
    private Set<String> contextTags;
    
    /**
     * 临时数据存储
     */
    private Map<String, Object> temporaryData;
    
    /**
     * 对话状态枚举
     */
    public enum ConversationState {
        /**
         * 初始状态
         */
        INITIAL,
        
        /**
         * 需求收集中
         */
        REQUIREMENT_GATHERING,
        
        /**
         * 推荐生成中
         */
        RECOMMENDATION_GENERATING,
        
        /**
         * 推荐展示中
         */
        RECOMMENDATION_SHOWING,
        
        /**
         * 澄清中（需要更多信息）
         */
        CLARIFYING,
        
        /**
         * 对话结束
         */
        FINISHED,
        
        /**
         * 异常状态
         */
        ERROR
    }
    
    /**
     * 添加对话消息到历史记录
     */
    public void addConversationMessage(ConversationMessage message) {
        if (conversationHistory == null) {
            conversationHistory = new ArrayList<>();
        }
        conversationHistory.add(message);
        updateLastActiveTime();
    }
    
    /**
     * 获取最近的对话历史
     */
    public List<ConversationMessage> getRecentHistory(int count) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return new ArrayList<>();
        }
        int startIndex = Math.max(0, conversationHistory.size() - count);
        return conversationHistory.subList(startIndex, conversationHistory.size());
    }
    
    /**
     * 更新用户偏好
     */
    public void updateUserPreference(String key, Object value) {
        if (userPreferences == null) {
            userPreferences = new HashMap<>();
        }
        userPreferences.put(key, value);
    }
    
    /**
     * 获取用户偏好
     */
    @SuppressWarnings("unchecked")
    public <T> T getUserPreference(String key, T defaultValue) {
        if (userPreferences == null) {
            return defaultValue;
        }
        return (T) userPreferences.getOrDefault(key, defaultValue);
    }
    
    /**
     * 添加上下文标签
     */
    public void addContextTag(String tag) {
        if (contextTags == null) {
            contextTags = new HashSet<>();
        }
        contextTags.add(tag);
    }
    
    /**
     * 检查是否包含标签
     */
    public boolean hasContextTag(String tag) {
        return contextTags != null && contextTags.contains(tag);
    }
    
    /**
     * 设置临时数据
     */
    public void setTemporaryData(String key, Object value) {
        if (temporaryData == null) {
            temporaryData = new HashMap<>();
        }
        temporaryData.put(key, value);
    }
    
    /**
     * 获取临时数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getTemporaryData(String key) {
        if (temporaryData == null) {
            return null;
        }
        return (T) temporaryData.get(key);
    }
    
    /**
     * 更新最后活跃时间
     */
    public void updateLastActiveTime() {
        this.lastActiveTime = LocalDateTime.now();
    }
    
    /**
     * 增加对话轮次
     */
    public void incrementConversationRound() {
        if (conversationRound == null) {
            conversationRound = 1;
        } else {
            conversationRound++;
        }
    }
    
    /**
     * 检查对话是否超时（超过30分钟无活动）
     */
    public boolean isExpired() {
        if (lastActiveTime == null) {
            return false;
        }
        return lastActiveTime.plusMinutes(30).isBefore(LocalDateTime.now());
    }
    
    /**
     * 重置上下文（保留基本信息）
     */
    public void reset() {
        this.currentState = ConversationState.INITIAL;
        this.userRequirement = null;
        this.currentRecommendations = null;
        this.conversationRound = 0;
        if (this.temporaryData != null) {
            this.temporaryData.clear();
        }
        updateLastActiveTime();
    }
    
    /**
     * 获取上下文摘要信息
     */
    public String getContextSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("会话ID: ").append(sessionId).append(", ");
        summary.append("用户ID: ").append(userId).append(", ");
        summary.append("状态: ").append(currentState).append(", ");
        summary.append("轮次: ").append(conversationRound).append(", ");
        
        if (userRequirement != null) {
            summary.append("需求: ");
            if (userRequirement.getPeopleCount() != null) {
                summary.append(userRequirement.getPeopleCount()).append("人, ");
            }
            if (userRequirement.getCuisineType() != null) {
                summary.append(userRequirement.getCuisineType()).append(", ");
            }
            if (userRequirement.getBudgetRange() != null) {
                summary.append("预算").append(userRequirement.getBudgetRange()).append("元/人, ");
            }
        }
        
        if (currentRecommendations != null && !currentRecommendations.isEmpty()) {
            summary.append("推荐菜品数: ").append(currentRecommendations.size());
        }
        
        return summary.toString();
    }
} 