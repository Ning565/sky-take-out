package com.star.ai.agent;

import com.star.ai.agent.base.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Agent协调器 - 采用ReAct模式
 * 负责多Agent系统的调度与协作。
 * 实现A2A通信协议，支持Agent间数据共享。
 */
@Component
@Slf4j
public class AgentCoordinator {

    @Autowired
    private ApplicationContext applicationContext;

    private final Map<String, AIAgent> registeredAgents = new HashMap<>();
    private final List<AIAgent> orderedAgents = new ArrayList<>();

    /**
     * 初始化方法，自动注册所有AIAgent实现
     */
    @PostConstruct
    public void init() {
        log.info("开始初始化Agent协调器，注册所有Agent");
        
        // 获取Spring容器中所有AIAgent类型的Bean
        Map<String, AIAgent> agentBeans = applicationContext.getBeansOfType(AIAgent.class);
        
        for (Map.Entry<String, AIAgent> entry : agentBeans.entrySet()) {
            AIAgent agent = entry.getValue();
            String agentName = agent.getCapability().getName();
            registeredAgents.put(agentName, agent);
            log.info("注册Agent: {} (优先级: {})", agentName, agent.getPriority());
        }
        
        // 按优先级排序
        orderedAgents.addAll(registeredAgents.values());
        orderedAgents.sort((a1, a2) -> Integer.compare(a2.getPriority(), a1.getPriority()));
        
        log.info("Agent协调器初始化完成，注册{}个Agent", registeredAgents.size());
    }

    /**
     * 处理用户请求，协调多个Agent执行
     * @param request 用户请求
     * @param context Agent上下文
     * @return 最终处理结果
     */
    public AgentResponse coordinate(AgentRequest request, AgentContext context) {
        try {
            log.info("Agent协调器开始处理请求: {}", request.getRequestId());
            
            // ReAct模式：Reasoning（推理） -> Acting（行动） -> Observing（观察）
            
            // 1. Reasoning: 分析执行策略
            String executionPlan = performReasoning(request, context);
            log.debug("执行计划: {}", executionPlan);
            
            // 2. Acting: 执行Agent链
            AgentResponse finalResponse = performAgentExecution(request, context, executionPlan);
            
            // 3. Observing: 验证最终结果
            boolean isValid = validateFinalResponse(finalResponse);
            
            if (!isValid) {
                log.warn("最终结果验证失败: {}", finalResponse.getMessage());
                return AgentResponse.error("处理失败: " + finalResponse.getMessage());
            }
            
            log.info("Agent协调器处理完成: {}", request.getRequestId());
            return finalResponse;
            
        } catch (Exception e) {
            log.error("Agent协调器处理失败", e);
            return AgentResponse.error("协调处理失败: " + e.getMessage());
        }
    }

    /**
     * ReAct - Reasoning: 分析执行策略
     */
    private String performReasoning(AgentRequest request, AgentContext context) {
        StringBuilder plan = new StringBuilder();
        plan.append("执行计划:\n");
        
        for (int i = 0; i < orderedAgents.size(); i++) {
            AIAgent agent = orderedAgents.get(i);
            plan.append(String.format("%d. %s (优先级: %d)\n", 
                i + 1, agent.getCapability().getName(), agent.getPriority()));
        }
        
        return plan.toString();
    }

    /**
     * ReAct - Acting: 执行Agent链
     */
    private AgentResponse performAgentExecution(AgentRequest request, AgentContext context, String executionPlan) {
        AgentResponse lastResponse = null;
        
        for (AIAgent agent : orderedAgents) {
            String agentName = agent.getCapability().getName();
            log.info("执行Agent: {}", agentName);
            
            try {
                AgentResponse response = agent.process(request, context);
                lastResponse = response;
                
                if (response.isError()) {
                    log.warn("Agent {} 执行失败: {}", agentName, response.getMessage());
                    return response;
                } else if (response.isNeedMoreInfo()) {
                    log.info("Agent {} 需要更多信息: {}", agentName, response.getMessage());
                    return response;
                } else {
                    log.info("Agent {} 执行成功", agentName);
                }
            } catch (Exception e) {
                log.error("Agent {} 执行异常", agentName, e);
                return AgentResponse.error(String.format("%s执行失败: %s", agentName, e.getMessage()));
            }
        }
        
        return lastResponse;
    }

    /**
     * ReAct - Observing: 验证最终结果
     */
    private boolean validateFinalResponse(AgentResponse response) {
        if (response == null) {
            return false;
        }
        
        if (response.isError() || response.isNeedMoreInfo()) {
            return false;
        }
        
        return response.getData() != null;
    }

    /**
     * 获取注册的Agent
     * @param agentName Agent名称
     * @return Agent实例
     */
    public AIAgent getAgent(String agentName) {
        return registeredAgents.get(agentName);
    }

    /**
     * 获取所有注册的Agent
     * @return Agent列表
     */
    public List<AIAgent> getAllAgents() {
        return new ArrayList<>(registeredAgents.values());
    }

    /**
     * 手动注册Agent
     * @param agent Agent实例
     */
    public void registerAgent(AIAgent agent) {
        if (agent != null) {
            String agentName = agent.getCapability().getName();
            registeredAgents.put(agentName, agent);
            orderedAgents.add(agent);
            orderedAgents.sort((a1, a2) -> Integer.compare(a2.getPriority(), a1.getPriority()));
            log.info("手动注册Agent: {}", agentName);
        }
    }
} 