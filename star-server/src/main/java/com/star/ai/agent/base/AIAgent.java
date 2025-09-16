package com.star.ai.agent.base;

/**
 * Agent基础接口
 * 约定多Agent系统的统一处理方法。
 */
public interface AIAgent {
    
    /**
     * Agent处理逻辑
     * @param request 请求对象
     * @param context Agent上下文
     * @return 处理结果
     */
    AgentResponse process(AgentRequest request, AgentContext context);
    
    /**
     * 获取Agent能力描述
     * @return Agent能力描述对象
     */
    AgentCapability getCapability();
    
    /**
     * 获取Agent优先级
     * 优先级越高，在调度时越先执行
     * @return 优先级数值（数值越大优先级越高）
     */
    int getPriority();
} 