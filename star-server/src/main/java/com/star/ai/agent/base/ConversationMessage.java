package com.star.ai.agent.base;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 对话消息
 * 封装对话过程中的消息信息。
 */
@Data
@Builder
public class ConversationMessage {
    /**
     * 消息类型：用户消息
     */
    public static final String TYPE_USER = "user";
    
    /**
     * 消息类型：系统消息
     */
    public static final String TYPE_SYSTEM = "system";
    
    /**
     * 消息类型：助手消息
     */
    public static final String TYPE_ASSISTANT = "assistant";
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 消息类型
     */
    private String messageType;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 发送时间
     */
    private LocalDateTime timestamp;
    
    /**
     * 附加信息
     */
    private Map<String, Object> metadata;
    
    /**
     * 创建用户消息
     * @param content 消息内容
     * @return 用户消息对象
     */
    public static ConversationMessage userMessage(String content) {
        return ConversationMessage.builder()
                .messageType(TYPE_USER)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建系统消息
     * @param content 消息内容
     * @return 系统消息对象
     */
    public static ConversationMessage systemMessage(String content) {
        return ConversationMessage.builder()
                .messageType(TYPE_SYSTEM)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建助手消息
     * @param content 消息内容
     * @return 助手消息对象
     */
    public static ConversationMessage assistantMessage(String content) {
        return ConversationMessage.builder()
                .messageType(TYPE_ASSISTANT)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 判断是否为用户消息
     * @return 是否为用户消息
     */
    public boolean isUserMessage() {
        return TYPE_USER.equals(messageType);
    }
    
    /**
     * 判断是否为系统消息
     * @return 是否为系统消息
     */
    public boolean isSystemMessage() {
        return TYPE_SYSTEM.equals(messageType);
    }
    
    /**
     * 判断是否为助手消息
     * @return 是否为助手消息
     */
    public boolean isAssistantMessage() {
        return TYPE_ASSISTANT.equals(messageType);
    }
} 