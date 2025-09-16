package com.star.ai.agent.tool;

import java.util.Map;

/**
 * 通用AI Tool接口，便于统一注册和调用
 * 参考OpenAI function-calling风格
 */
public interface Tool {
    /**
     * 工具名称
     */
    String getName();

    /**
     * 工具描述
     */
    String getDescription();

    /**
     * 工具参数定义（OpenAI function-calling风格）
     */
    Map<String, Object> getParameterSchema();

    /**
     * 工具调用入口
     * @param params 结构化参数
     * @return 结构化结果
     */
    Object call(Map<String, Object> params);
} 