package com.star.ai.agent.base;

import lombok.Data;
import lombok.Builder;
import java.util.Map;

/**
 * Agent请求
 * 封装发送给Agent的请求信息。
 */
@Data
@Builder
public class AgentRequest {
    /**
     * 请求ID
     */
    private String requestId;
    
    /**
     * 用户消息
     */
    private String userMessage;
    
    /**
     * 请求类型
     */
    private String requestType;
    
    /**
     * 请求参数
     */
    private Map<String, Object> parameters;
    
    /**
     * 获取请求参数
     * @param key 参数键
     * @param <T> 参数类型
     * @return 参数值
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key) {
        if (parameters == null) {
            return null;
        }
        return (T) parameters.get(key);
    }
    
    /**
     * 获取请求参数，如果不存在则返回默认值
     * @param key 参数键
     * @param defaultValue 默认值
     * @param <T> 参数类型
     * @return 参数值或默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, T defaultValue) {
        if (parameters == null || !parameters.containsKey(key)) {
            return defaultValue;
        }
        return (T) parameters.get(key);
    }
} 