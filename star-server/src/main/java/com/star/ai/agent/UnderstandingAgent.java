package com.star.ai.agent;

import com.star.ai.agent.base.*;
import com.star.pojo.dto.UserRequirement;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ai.chat.ChatClient;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.List;

/**
 * 理解Agent - 采用ReAct模式
 * 负责用户需求的自然语言理解与结构化提取。
 * 实现A2A通信协议，支持Agent间协作。
 */
@Component
@Slf4j
public class UnderstandingAgent implements AIAgent {
    
    @Autowired
    private ChatClient chatClient;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public AgentResponse process(AgentRequest request, AgentContext context) {
        try {
            log.info("UnderstandingAgent开始处理请求: {}", request.getRequestId());
            
            // ReAct模式：Reasoning（推理） -> Acting（行动） -> Observing（观察）
            
            // 1. Reasoning: 分析用户输入，决定处理策略
            String reasoning = performReasoning(request.getUserMessage(), context);
            log.debug("推理结果: {}", reasoning);
            
            // 2. Acting: 执行结构化提取
            UserRequirement userRequirement = performExtraction(request.getUserMessage(), context, reasoning);
            
            // 3. Observing: 验证提取结果
            boolean isValid = validateExtraction(userRequirement);
            
            if (!isValid) {
                return AgentResponse.needMoreInfo("需要更多信息来理解您的需求", "请提供更具体的用餐需求");
            }
            
            // 4. 保存到共享上下文 (A2A通信)
            context.setSharedData("userRequirement", userRequirement);
            context.setSharedData("understandingResult", "completed");
            
            // 5. 添加对话历史
            context.addToHistory(ConversationMessage.userMessage(request.getUserMessage()));
            context.addToHistory(ConversationMessage.assistantMessage("已理解您的需求"));
            
            log.info("UnderstandingAgent处理完成，提取需求: {}", userRequirement);
            
            return AgentResponse.success("需求理解完成", userRequirement);
            
        } catch (Exception e) {
            log.error("UnderstandingAgent处理失败", e);
            return AgentResponse.error("需求理解失败: " + e.getMessage());
        }
    }
    
    /**
     * ReAct - Reasoning: 推理用户意图和处理策略
     */
    private String performReasoning(String userMessage, AgentContext context) {
        String reasoningPrompt = String.format("""
            你是一个专业的餐饮需求分析师。请分析用户消息，判断用户意图和处理策略。
            
            用户消息：%s
            
            请分析：
            1. 用户的主要意图是什么？
            2. 消息中包含哪些关键信息？
            3. 还缺少哪些重要信息？
            4. 应该采用什么提取策略？
            
            请用简洁的文字回答：
            """, userMessage);
        
        return chatClient.call(reasoningPrompt);
    }
    
    /**
     * ReAct - Acting: 执行结构化提取
     */
    private UserRequirement performExtraction(String userMessage, AgentContext context, String reasoning) {
        // 获取历史对话增强理解
        List<ConversationMessage> recentHistory = context.getRecentHistory(3);
        String conversationHistory = buildConversationHistory(recentHistory);
        
        String extractionPrompt = String.format("""
            你是一个专业的餐饮需求理解助手。请从用户消息中提取结构化的用餐需求信息。
            
            推理分析：%s
            
            历史对话：%s
            
            当前用户消息：%s
            
            请提取以下信息并以JSON格式返回：
            {
                "peopleCount": "用餐人数（整数，如果未明确说明则为null）",
                "diningPurpose": "用餐目的（聚餐/商务/约会/家庭/快餐等）",
                "tastePreferences": ["口味偏好数组（辣/甜/清淡/重口/酸/麻等）"],
                "cuisineType": "菜系偏好（川菜/粤菜/湘菜/鲁菜/苏菜/浙菜/闽菜/徽菜/东北菜/西餐等）",
                "budgetRange": "预算范围（人均预算，整数，如100表示人均100元）",
                "dietaryRestrictions": ["饮食禁忌（素食/不吃辣/不吃海鲜/不吃牛肉等）"],
                "mealTime": "用餐时间（早餐/午餐/晚餐/夜宵）",
                "specialNeeds": ["特殊需求（聚餐/商务宴请/约会/孩子用餐等）"]
            }
            
            注意：
            1. 如果某个信息用户没有提到，对应字段设为null或空数组
            2. 尽量推理用户的隐含需求
            3. 只返回JSON，不要其他解释文本
            
            JSON结果：
            """, reasoning, conversationHistory, userMessage);
        
        try {
            String llmResponse = chatClient.call(extractionPrompt);
            String jsonPart = extractJSON(llmResponse);
            return objectMapper.readValue(jsonPart, UserRequirement.class);
        } catch (Exception e) {
            log.error("解析用户需求失败：{}", e.getMessage(), e);
            return createDefaultRequirement();
        }
    }
    
    /**
     * ReAct - Observing: 验证提取结果
     */
    private boolean validateExtraction(UserRequirement requirement) {
        // 基本验证：至少要有一个有效信息
        return requirement != null && (
            requirement.getPeopleCount() != null ||
            requirement.getDiningPurpose() != null ||
            (requirement.getTastePreferences() != null && !requirement.getTastePreferences().isEmpty()) ||
            requirement.getCuisineType() != null ||
            requirement.getBudgetRange() != null ||
            (requirement.getDietaryRestrictions() != null && !requirement.getDietaryRestrictions().isEmpty()) ||
            requirement.getMealTime() != null ||
            (requirement.getSpecialNeeds() != null && !requirement.getSpecialNeeds().isEmpty())
        );
    }
    
    /**
     * 构建对话历史字符串
     */
    private String buildConversationHistory(List<ConversationMessage> history) {
        if (history == null || history.isEmpty()) {
            return "无历史对话";
        }
        
        StringBuilder sb = new StringBuilder();
        for (ConversationMessage msg : history) {
            sb.append(String.format("[%s]: %s\n", msg.getMessageType(), msg.getContent()));
        }
        return sb.toString();
    }
    
    /**
     * 提取JSON部分
     */
    private String extractJSON(String response) {
        // 查找JSON开始和结束标记
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }
    
    /**
     * 创建默认需求对象
     */
    private UserRequirement createDefaultRequirement() {
        UserRequirement requirement = new UserRequirement();
        // 设置默认值
        return requirement;
    }
    
    @Override
    public AgentCapability getCapability() {
        return AgentCapability.builder()
                .name("UnderstandingAgent")
                .description("自然语言理解和需求提取Agent，采用ReAct模式")
                .inputType("自然语言文本")
                .outputType("结构化用户需求 (UserRequirement)")
                .supportedFeatures(Arrays.asList("NLU", "需求提取", "意图识别", "上下文理解"))
                .version("1.0.0")
                .build();
    }
    
    @Override
    public int getPriority() {
        return 100; // 最高优先级，通常最先执行
    }
} 