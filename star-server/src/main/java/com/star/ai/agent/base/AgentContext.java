package com.star.ai.agent.base;

import lombok.Data;
import lombok.Builder;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Agent上下文
 * 封装多Agent协作过程中的上下文信息。
 */
@Data
@Builder
public class AgentContext {
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 对话历史记录
     */
    private List<ConversationMessage> history;
    
    /**
     * Spring AI ChatClient
     * 用于Agent调用大模型
     */
    private ChatClient chatClient;
    
    /**
     * Spring AI VectorStore
     * 用于Agent进行向量检索
     */
    private VectorStore vectorStore;
    
    /**
     * Agent间共享数据
     * 用于多Agent协作时共享信息
     */
    private Map<String, Object> sharedData;
    
    /**
     * 获取共享数据
     * @param key 数据键
     * @param <T> 数据类型
     * @return 数据值
     */
    @SuppressWarnings("unchecked")
    public <T> T getSharedData(String key) {
        if (sharedData == null) {
            return null;
        }
        return (T) sharedData.get(key);
    }
    
    /**
     * 设置共享数据
     * @param key 数据键
     * @param value 数据值
     */
    public void setSharedData(String key, Object value) {
        if (sharedData == null) {
            sharedData = new HashMap<>();
        }
        sharedData.put(key, value);
    }
    
    /**
     * 添加对话消息到历史记录
     * @param message 对话消息
     */
    public void addToHistory(ConversationMessage message) {
        if (history == null) {
            history = new ArrayList<>();
        }
        history.add(message);
    }
    
    /**
     * 获取最近的历史消息
     * @param count 消息数量
     * @return 最近的历史消息列表
     */
    public List<ConversationMessage> getRecentHistory(int count) {
        if (history == null || history.isEmpty()) {
            return new ArrayList<>();
        }
        int startIndex = Math.max(0, history.size() - count);
        return history.subList(startIndex, history.size());
    }
} 