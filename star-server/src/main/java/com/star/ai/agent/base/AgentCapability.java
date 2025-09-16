package com.star.ai.agent.base;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * Agent能力描述
 * 用于描述Agent的功能、输入输出类型等。
 */
@Data
@Builder
public class AgentCapability {
    /**
     * Agent名称
     */
    private String name;
    
    /**
     * Agent描述
     */
    private String description;
    
    /**
     * 输入类型描述
     */
    private String inputType;
    
    /**
     * 输出类型描述
     */
    private String outputType;
    
    /**
     * Agent支持的功能列表
     */
    private List<String> supportedFeatures;
    
    /**
     * Agent版本
     */
    private String version;
    
    /**
     * 是否支持特定功能
     * @param feature 功能名称
     * @return 是否支持
     */
    public boolean supportsFeature(String feature) {
        return supportedFeatures != null && supportedFeatures.contains(feature);
    }
    
    /**
     * 获取Agent能力的简要描述
     * @return 简要描述字符串
     */
    public String getCapabilityDescription() {
        return String.format("%s: %s (输入: %s, 输出: %s)", 
                name, description, inputType, outputType);
    }
} 