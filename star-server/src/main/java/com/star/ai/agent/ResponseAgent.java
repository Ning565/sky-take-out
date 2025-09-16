package com.star.ai.agent;

import com.star.ai.agent.base.*;
import com.star.pojo.dto.UserRequirement;
import com.star.pojo.vo.RecommendationResult;
import com.star.pojo.vo.ChatResponse;
import com.star.pojo.vo.DishRecommendation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ai.chat.ChatClient;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 回答Agent - 采用ReAct模式
 * 负责将推荐结果转化为自然语言回复。
 * 结合function calling技术，实现A2A通信协议，支持Agent间协作。
 */
@Component
@Slf4j
public class ResponseAgent implements AIAgent {
    
    @Autowired
    private ChatClient chatClient;
    
    @Override
    public AgentResponse process(AgentRequest request, AgentContext context) {
        try {
            log.info("ResponseAgent开始处理请求: {}", request.getRequestId());
            
            // ReAct模式：Reasoning（推理） -> Acting（行动） -> Observing（观察）
            
            // 1. 获取推荐Agent的结果 (A2A通信)
            RecommendationResult recommendation = context.getSharedData("recommendationResult");
            UserRequirement requirement = context.getSharedData("userRequirement");
            
            if (recommendation == null) {
                return handleNoRecommendation(requirement, context);
            }
            
            // 2. Reasoning: 分析回复策略
            String reasoning = performReasoning(recommendation, requirement, request.getUserMessage(), context);
            log.debug("回复推理结果: {}", reasoning);
            
            // 3. Acting: 生成自然语言回复
            ChatResponse chatResponse = performResponseGeneration(recommendation, requirement, context, reasoning);
            
            // 4. Observing: 验证回复质量
            boolean isValid = validateResponse(chatResponse, recommendation, requirement);
            
            if (!isValid) {
                return AgentResponse.error("生成的回复质量不符合要求");
            }
            
            // 5. 保存到共享上下文 (A2A通信)
            context.setSharedData("finalResponse", chatResponse);
            context.setSharedData("responseCompleted", "completed");
            
            // 6. 添加对话历史
            context.addToHistory(ConversationMessage.assistantMessage(chatResponse.getMessageText()));
            
            log.info("ResponseAgent处理完成，生成回复长度: {}", chatResponse.getMessageText().length());
            
            return AgentResponse.success("回复生成完成", chatResponse);
            
        } catch (Exception e) {
            log.error("ResponseAgent处理失败", e);
            return AgentResponse.error("回复生成失败: " + e.getMessage());
        }
    }
    
    /**
     * ReAct - Reasoning: 分析回复策略
     */
    private String performReasoning(RecommendationResult recommendation, UserRequirement requirement, 
                                  String userMessage, AgentContext context) {
        String reasoningPrompt = String.format("""
            你是一个专业的餐厅服务员。请分析如何为客人提供最佳的回复体验。
            
            推荐结果：
            - 推荐菜品数量：%d
            - 总价格：%.2f元
            - 置信度：%.1f%%
            
            用户需求：
            - 用餐人数：%s人
            - 预算：%s元/人
            - 口味偏好：%s
            - 菜系偏好：%s
            
            用户消息：%s
            
            请分析：
            1. 应该采用什么语气和风格？
            2. 重点突出哪些卖点？
            3. 如何让回复更有吸引力？
            4. 是否需要提供额外建议？
            5. 如何引导后续对话？
            
            请用简洁的文字回答：
            """, 
            recommendation.getDishes().size(),
            recommendation.getTotalPrice(),
            recommendation.getConfidenceScore(),
            requirement.getPeopleCount(),
            requirement.getBudgetRange(),
            requirement.getTastePreferences(),
            requirement.getCuisineType(),
            userMessage);
        
        return chatClient.call(reasoningPrompt);
    }
    
    /**
     * ReAct - Acting: 生成自然语言回复
     */
    private ChatResponse performResponseGeneration(RecommendationResult recommendation, UserRequirement requirement, 
                                                 AgentContext context, String reasoning) {
        // 1. 构建推荐菜品列表字符串
        StringBuilder dishList = new StringBuilder();
        for (int i = 0; i < recommendation.getDishes().size(); i++) {
            DishRecommendation dish = recommendation.getDishes().get(i);
            dishList.append(String.format("%d. %s（%.2f元）：%s\n", 
                i + 1,
                dish.getDish().getName(), 
                dish.getDish().getPrice(),
                dish.getReason()
            ));
        }
        
        // 2. 计算人均价格
        BigDecimal avgPrice = BigDecimal.ZERO;
        if (requirement.getPeopleCount() != null && requirement.getPeopleCount() > 0) {
            avgPrice = recommendation.getTotalPrice().divide(
                BigDecimal.valueOf(requirement.getPeopleCount()), 2, RoundingMode.HALF_UP);
        }
        
        // 3. 构建回复生成Prompt (支持Function Calling扩展)
        String responsePrompt = buildResponsePrompt(dishList.toString(), recommendation, requirement, avgPrice, reasoning);
        
        // 4. 调用大模型生成自然语言回复
        String naturalResponse = chatClient.call(responsePrompt);
        
        // 5. 生成后续问题
        List<String> followUpQuestions = generateFollowUpQuestions(recommendation, requirement, context);
        
        // 6. 构建完整回复对象
        return ChatResponse.builder()
                .messageText(naturalResponse)
                .recommendations(recommendation.getDishes())
                .followUpQuestions(followUpQuestions)
                .sessionId(context.getSessionId())
                .totalPrice(recommendation.getTotalPrice())
                .avgPrice(avgPrice)
                .confidenceScore(recommendation.getConfidenceScore())
                .build();
    }
    
    /**
     * ReAct - Observing: 验证回复质量
     */
    private boolean validateResponse(ChatResponse response, RecommendationResult recommendation, UserRequirement requirement) {
        if (response == null || response.getMessageText() == null || response.getMessageText().trim().isEmpty()) {
            return false;
        }
        
        String text = response.getMessageText();
        
        // 检查回复长度是否合理（不能太短或太长）
        if (text.length() < 20 || text.length() > 1000) {
            return false;
        }
        
        // 检查是否包含推荐的菜品名称
        boolean containsDishName = recommendation.getDishes().stream()
                .anyMatch(dish -> text.contains(dish.getDish().getName()));
        
        if (!containsDishName) {
            return false;
        }
        
        // 检查是否包含价格信息
        if (!text.contains("元") && !text.contains("价格")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 构建回复生成Prompt
     */
    private String buildResponsePrompt(String dishList, RecommendationResult recommendation, 
                                     UserRequirement requirement, BigDecimal avgPrice, String reasoning) {
        return String.format("""
            你是一个专业友好的餐厅服务员。请为客人介绍推荐的菜品。
            
            策略分析：%s
            
            客人需求：%s人用餐，喜欢%s口味，偏好%s菜系
            
            推荐菜品：
            %s
            
            总价：%.2f元（人均%.2f元）
            推荐置信度：%.1f%%
            
            要求：
            1. 语气要亲切自然，像真实服务员一样
            2. 简要介绍推荐的菜品特色
            3. 说明为什么适合客人的需求
            4. 提到总价和人均价格
            5. 整体回复控制在100-200字
            6. 不要使用"推荐指数"等生硬词汇
            7. 可以适当加入服务员的专业建议
            
            Function Calling支持：
            - 如果需要查询菜品详情，可调用getDishDetail函数
            - 如果需要检查库存，可调用checkInventory函数
            - 如果需要计算优惠，可调用calculateDiscount函数
            
            回复：
            """, 
            reasoning,
            requirement.getPeopleCount(),
            requirement.getTastePreferences(),
            requirement.getCuisineType(),
            dishList,
            recommendation.getTotalPrice(),
            avgPrice,
            recommendation.getConfidenceScore()
        );
    }
    
    /**
     * 生成后续问题
     */
    private List<String> generateFollowUpQuestions(RecommendationResult recommendation, 
                                                 UserRequirement requirement, AgentContext context) {
        List<String> questions = new ArrayList<>();
        
        // 基于需求生成个性化问题
        if (requirement.getBudgetRange() == null) {
            questions.add("对价格有什么要求吗？");
        }
        
        // 检查是否有汤类推荐
        boolean hasSoup = recommendation.getDishes().stream()
                .anyMatch(d -> d.getDish().getName().contains("汤") || 
                             d.getDish().getDescription().contains("汤"));
        if (!hasSoup) {
            questions.add("需要配个汤吗？我们有很棒的汤品推荐。");
        }
        
        // 检查饮食禁忌
        if (requirement.getDietaryRestrictions() == null || requirement.getDietaryRestrictions().isEmpty()) {
            questions.add("有什么忌口的吗？");
        }
        
        // 通用问题
        questions.add("这些菜品您觉得怎么样？");
        questions.add("需要我为您详细介绍某道菜吗？");
        questions.add("还需要其他什么帮助吗？");
        
        // 返回前3个最相关的问题
        return questions.subList(0, Math.min(3, questions.size()));
    }
    
    /**
     * 处理无推荐结果的情况
     */
    private AgentResponse handleNoRecommendation(UserRequirement requirement, AgentContext context) {
        String fallbackMessage = "很抱歉，根据您当前的需求，暂时没有找到特别合适的推荐。";
        
        if (requirement != null) {
            fallbackMessage += "不过我可以为您提供一些建议：";
            
            if (requirement.getBudgetRange() != null) {
                fallbackMessage += String.format("您的预算是%d元/人，", requirement.getBudgetRange());
            }
            
            if (requirement.getCuisineType() != null) {
                fallbackMessage += String.format("您喜欢%s，", requirement.getCuisineType());
            }
            
            fallbackMessage += "请问能否告诉我更多详细的用餐需求？比如具体的口味偏好或特殊要求？";
        } else {
            fallbackMessage += "请告诉我您的用餐需求，比如用餐人数、口味偏好、预算等，我会为您推荐合适的菜品。";
        }
        
        ChatResponse response = ChatResponse.builder()
                .messageText(fallbackMessage)
                .sessionId(context.getSessionId())
                .followUpQuestions(Arrays.asList("您有什么特别的口味偏好吗？", "用餐预算大概是多少？", "有什么忌口的吗？"))
                .build();
        
        return AgentResponse.success("生成fallback回复", response);
    }
    
    @Override
    public AgentCapability getCapability() {
        return AgentCapability.builder()
                .name("ResponseAgent")
                .description("自然语言生成回复Agent，采用ReAct模式，支持Function Calling")
                .inputType("推荐结果 (RecommendationResult)")
                .outputType("对话回复 (ChatResponse)")
                .supportedFeatures(Arrays.asList("自然语言生成", "对话管理", "Function Calling", "后续问题生成"))
                .version("1.0.0")
                .build();
    }
    
    @Override
    public int getPriority() {
        return 60; // 较低优先级，最后执行
    }
} 