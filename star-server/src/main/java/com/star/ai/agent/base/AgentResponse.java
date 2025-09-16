package com.star.ai.agent.base;

import lombok.Data;
import lombok.Builder;
import java.util.Map;
import java.util.HashMap;

/**
 * Agent响应
 * 封装Agent处理后的响应信息。
 */
@Data
@Builder
public class AgentResponse {
    /**
     * 响应状态：成功
     */
    public static final String STATUS_SUCCESS = "success";
    
    /**
     * 响应状态：失败
     */
    public static final String STATUS_ERROR = "error";
    
    /**
     * 响应状态：需要更多信息
     */
    public static final String STATUS_NEED_MORE_INFO = "need_more_info";
    
    /**
     * 响应ID
     */
    private String responseId;
    
    /**
     * 响应状态
     */
    private String status;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private Object data;
    
    /**
     * 附加信息
     */
    private Map<String, Object> additionalInfo;
    
    /**
     * 创建成功响应
     * @param message 响应消息
     * @param data 响应数据
     * @return 成功响应对象
     */
    public static AgentResponse success(String message, Object data) {
        return AgentResponse.builder()
                .status(STATUS_SUCCESS)
                .message(message)
                .data(data)
                .build();
    }
    
    /**
     * 创建成功响应
     * @param message 响应消息
     * @return 成功响应对象
     */
    public static AgentResponse success(String message) {
        return success(message, null);
    }
    
    /**
     * 创建失败响应
     * @param message 错误消息
     * @return 失败响应对象
     */
    public static AgentResponse error(String message) {
        return AgentResponse.builder()
                .status(STATUS_ERROR)
                .message(message)
                .build();
    }
    
    /**
     * 创建需要更多信息的响应
     * @param message 提示消息
     * @param requiredInfo 需要的信息描述
     * @return 需要更多信息的响应对象
     */
    public static AgentResponse needMoreInfo(String message, String requiredInfo) {
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("requiredInfo", requiredInfo);
        
        return AgentResponse.builder()
                .status(STATUS_NEED_MORE_INFO)
                .message(message)
                .additionalInfo(additionalInfo)
                .build();
    }
    
    /**
     * 添加附加信息
     * @param key 信息键
     * @param value 信息值
     * @return 当前响应对象
     */
    public AgentResponse addAdditionalInfo(String key, Object value) {
        if (additionalInfo == null) {
            additionalInfo = new HashMap<>();
        }
        additionalInfo.put(key, value);
        return this;
    }
    
    /**
     * 判断响应是否成功
     * @return 是否成功
     */
    public boolean isSuccess() {
        return STATUS_SUCCESS.equals(status);
    }
    
    /**
     * 判断响应是否失败
     * @return 是否失败
     */
    public boolean isError() {
        return STATUS_ERROR.equals(status);
    }
    
    /**
     * 判断响应是否需要更多信息
     * @return 是否需要更多信息
     */
    public boolean isNeedMoreInfo() {
        return STATUS_NEED_MORE_INFO.equals(status);
    }
} 