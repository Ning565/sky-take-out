package com.star.ai.agent;

import com.star.ai.agent.base.*;
import com.star.ai.rag.RAGService;
import com.star.ai.rag.VectorSearchService;
import com.star.pojo.dto.UserRequirement;
import com.star.pojo.vo.DishRecommendation;
import com.star.pojo.vo.RecommendationResult;
import com.star.pojo.entity.Dish;
import com.star.ai.agent.tool.ToolRegistry;
import com.star.ai.agent.tool.Tool;
import com.star.vo.RecipeNutritionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.document.Document;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;

/**
 * 推荐Agent - 采用ReAct模式
 * 负责基于RAG的智能推荐。
 * 实现A2A通信协议，支持Agent间协作。
 */
@Component
@Slf4j
public class RecommendationAgent implements AIAgent {
    
    @Autowired
    private ChatClient chatClient;
    
    @Autowired
    private RAGService ragService;
    
    @Autowired
    private VectorSearchService vectorSearchService;
    
    @Autowired
    private ToolRegistry toolRegistry;
    
    @Override
    public AgentResponse process(AgentRequest request, AgentContext context) {
        try {
            log.info("RecommendationAgent开始处理请求: {}", request.getRequestId());
            
            // ReAct模式：Reasoning（推理） -> Acting（行动） -> Observing（观察）
            
            // 1. 获取理解Agent的结果 (A2A通信)
            UserRequirement requirement = context.getSharedData("userRequirement");
            if (requirement == null) {
                return AgentResponse.error("缺少用户需求信息，请先运行理解Agent");
            }
            
            // 2. Reasoning: 分析推荐策略
            String reasoning = performReasoning(requirement, request.getUserMessage(), context);
            log.debug("推荐推理结果: {}", reasoning);
            
            // 3. Acting: 执行RAG推荐（使用完整的推荐流程）
            RecommendationResult recommendationResult = performRecommendation(requirement, request.getUserMessage(), context, reasoning);
            
            // 4. Observing: 验证推荐结果
            boolean isValid = validateRecommendation(recommendationResult, requirement);
            
            if (!isValid) {
                return AgentResponse.error("推荐结果验证失败，未找到合适的推荐");
            }
            
            // 5. 保存到共享上下文 (A2A通信)
            context.setSharedData("recommendationResult", recommendationResult);
            context.setSharedData("recommendationCompleted", "completed");
            
            log.info("RecommendationAgent处理完成，推荐{}道菜品", recommendationResult.getDishes().size());
            
            return AgentResponse.success("推荐生成完成", recommendationResult);
            
        } catch (Exception e) {
            log.error("RecommendationAgent处理失败", e);
            return AgentResponse.error("推荐Agent异常: " + e.getMessage());
        }
    }
    
    /**
     * ReAct - Reasoning: 分析推荐策略
     */
    private String performReasoning(UserRequirement requirement, String userMessage, AgentContext context) {
        String reasoningPrompt = String.format("""
            你是一个专业的餐饮推荐策略师。请基于用户需求分析最佳推荐策略。
            
            用户需求：
            - 用餐人数：%s
            - 用餐目的：%s
            - 口味偏好：%s
            - 菜系偏好：%s
            - 预算范围：%s
            - 饮食禁忌：%s
            - 用餐时间：%s
            - 特殊需求：%s
            
            用户消息：%s
            
            请分析：
            1. 应该重点关注哪些推荐维度？
            2. 搜索关键词应该如何构建？
            3. 过滤条件应该如何设置？
            4. 推荐数量应该是多少？
            
            请用简洁的文字回答：
            """, 
            requirement.getPeopleCount(),
            requirement.getDiningPurpose(),
            requirement.getTastePreferences(),
            requirement.getCuisineType(),
            requirement.getBudgetRange(),
            requirement.getDietaryRestrictions(),
            requirement.getMealTime(),
            requirement.getSpecialNeeds(),
            userMessage);
        
        return chatClient.call(reasoningPrompt);
    }
    
    /**
     * ReAct - Acting: 执行RAG检索和推荐
     */
    private RecommendationResult performRecommendation(UserRequirement requirement, String userMessage, 
                                                     AgentContext context, String reasoning) {
        // 1. 构建搜索查询
        String searchQuery = buildSearchQuery(requirement);
        log.debug("搜索查询: {}", searchQuery);
        
        // 2. RAG向量检索
        List<Document> relevantDocs = vectorSearchService.similaritySearch(searchQuery);
        log.debug("检索到{}个相关文档", relevantDocs.size());
        
        // 3. 基于内容过滤和排序
        List<DishRecommendation> filteredRecommendations = filterAndRankDishes(relevantDocs, requirement, reasoning);
        
        // 4. 可选：使用RecipeNutritionTool增强推荐（如果RAG结果不足）
        if (filteredRecommendations.isEmpty() || filteredRecommendations.size() < 3) {
            log.info("RAG推荐结果不足，尝试使用RecipeNutritionTool增强");
            List<DishRecommendation> toolRecommendations = enhanceWithTool(requirement);
            if (!toolRecommendations.isEmpty()) {
                filteredRecommendations.addAll(toolRecommendations);
                log.info("Tool增强推荐完成，新增{}道菜品", toolRecommendations.size());
            }
        }
        
        // 5. 使用大模型优化推荐理由
        List<DishRecommendation> enhancedRecommendations = enhanceWithLLM(filteredRecommendations, requirement, context);
        
        // 6. 构建最终推荐结果
        return RecommendationResult.builder()
                .dishes(enhancedRecommendations)
                .totalPrice(calculateTotalPrice(enhancedRecommendations))
                .confidenceScore(calculateConfidence(enhancedRecommendations))
                .searchQuery(searchQuery)
                .reasoning(reasoning)
                .build();
    }
    
    /**
     * 使用RecipeNutritionTool增强推荐
     */
    private List<DishRecommendation> enhanceWithTool(UserRequirement requirement) {
        List<DishRecommendation> toolRecommendations = new ArrayList<>();
        
        try {
            Tool tool = toolRegistry.getTool("RecipeNutritionTool");
            if (tool == null) {
                log.warn("RecipeNutritionTool未找到，跳过工具增强");
                return toolRecommendations;
            }
            
            // 参数转Map
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("peopleCount", requirement.getPeopleCount());
            paramMap.put("diningPurpose", requirement.getDiningPurpose());
            paramMap.put("tastePreferences", requirement.getTastePreferences());
            paramMap.put("cuisineType", requirement.getCuisineType());
            paramMap.put("budgetRange", requirement.getBudgetRange());
            paramMap.put("dietaryRestrictions", requirement.getDietaryRestrictions());
            paramMap.put("mealTime", requirement.getMealTime());
            paramMap.put("specialNeeds", requirement.getSpecialNeeds());
            
            RecipeNutritionResult result = (RecipeNutritionResult) tool.call(paramMap);
            
            if (result != null && result.getDishes() != null) {
                for (Object dishObj : result.getDishes()) {
                    if (dishObj instanceof Dish) {
                        Dish dish = (Dish) dishObj;
                        DishRecommendation recommendation = DishRecommendation.builder()
                                .dish(dish)
                                .score(75.0) // Tool推荐给予固定分数
                                .reason("通过营养工具分析推荐")
                                .build();
                        toolRecommendations.add(recommendation);
                    }
                }
            }
            
        } catch (Exception e) {
            log.warn("使用RecipeNutritionTool增强推荐失败: {}", e.getMessage());
        }
        
        return toolRecommendations;
    }
    
    /**
     * ReAct - Observing: 验证推荐结果
     */
    private boolean validateRecommendation(RecommendationResult recommendation, UserRequirement requirement) {
        if (recommendation == null || recommendation.getDishes() == null || recommendation.getDishes().isEmpty()) {
            return false;
        }
        
        // 检查推荐数量是否合理
        int dishCount = recommendation.getDishes().size();
        if (dishCount < 1 || dishCount > 10) {
            return false;
        }
        
        // 检查预算约束
        if (requirement.getBudgetRange() != null && requirement.getPeopleCount() != null) {
            BigDecimal totalBudget = BigDecimal.valueOf(requirement.getBudgetRange() * requirement.getPeopleCount());
            if (recommendation.getTotalPrice().compareTo(totalBudget.multiply(BigDecimal.valueOf(1.2))) > 0) {
                // 超出预算20%以上认为不合理
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 构建搜索查询
     */
    private String buildSearchQuery(UserRequirement requirement) {
        StringBuilder query = new StringBuilder();
        
        if (requirement.getCuisineType() != null) {
            query.append(requirement.getCuisineType()).append(" ");
        }
        
        if (requirement.getTastePreferences() != null && !requirement.getTastePreferences().isEmpty()) {
            query.append(String.join(" ", requirement.getTastePreferences())).append(" ");
        }
        
        if (requirement.getDiningPurpose() != null) {
            query.append(requirement.getDiningPurpose()).append(" ");
        }
        
        if (requirement.getMealTime() != null) {
            query.append(requirement.getMealTime()).append(" ");
        }
        
        return query.toString().trim();
    }
    
    /**
     * 过滤和排序菜品
     */
    private List<DishRecommendation> filterAndRankDishes(List<Document> docs, UserRequirement requirement, String reasoning) {
        List<DishRecommendation> recommendations = new ArrayList<>();
        
        for (Document doc : docs) {
            try {
                // 从Document元数据中构建Dish对象
                Dish dish = buildDishFromDocument(doc);
                
                // 应用过滤条件
                if (shouldFilterOut(dish, requirement)) {
                    continue;
                }
                
                // 计算推荐分数
                double score = calculateRecommendationScore(dish, requirement);
                
                DishRecommendation recommendation = DishRecommendation.builder()
                        .dish(dish)
                        .score(score)
                        .reason("基于您的需求推荐")
                        .build();
                
                recommendations.add(recommendation);
                
            } catch (Exception e) {
                log.warn("处理文档失败: {}", e.getMessage());
            }
        }
        
        // 排序并限制数量
        return recommendations.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(Math.min(10, Math.max(3, requirement.getPeopleCount() != null ? requirement.getPeopleCount() + 2 : 5)))
                .collect(Collectors.toList());
    }
    
    /**
     * 从Document构建Dish对象
     */
    private Dish buildDishFromDocument(Document doc) {
        Dish dish = new Dish();
        dish.setId(Long.valueOf(doc.getMetadata().getOrDefault("dishId", "0").toString()));
        dish.setName(doc.getMetadata().getOrDefault("dishName", "未知菜品").toString());
        dish.setPrice(BigDecimal.valueOf(Double.parseDouble(doc.getMetadata().getOrDefault("price", "0").toString())));
        dish.setCategoryId(Long.valueOf(doc.getMetadata().getOrDefault("categoryId", "0").toString()));
        dish.setDescription(doc.getContent());
        return dish;
    }
    
    /**
     * 判断是否应该过滤掉菜品
     */
    private boolean shouldFilterOut(Dish dish, UserRequirement requirement) {
        // 预算过滤
        if (requirement.getBudgetRange() != null) {
            if (dish.getPrice().doubleValue() > requirement.getBudgetRange() * 1.5) {
                return true;
            }
        }
        
        // 饮食禁忌过滤
        if (requirement.getDietaryRestrictions() != null) {
            for (String restriction : requirement.getDietaryRestrictions()) {
                if (dish.getDescription().contains(restriction) || dish.getName().contains(restriction)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 计算推荐分数
     */
    private double calculateRecommendationScore(Dish dish, UserRequirement requirement) {
        double score = 50.0; // 基础分数
        
        // 价格匹配度
        if (requirement.getBudgetRange() != null) {
            double priceRatio = dish.getPrice().doubleValue() / requirement.getBudgetRange();
            if (priceRatio <= 1.0) {
                score += 20;
            } else if (priceRatio <= 1.2) {
                score += 10;
            }
        }
        
        // 口味匹配度
        if (requirement.getTastePreferences() != null) {
            for (String taste : requirement.getTastePreferences()) {
                if (dish.getDescription().contains(taste) || dish.getName().contains(taste)) {
                    score += 15;
                }
            }
        }
        
        // 菜系匹配度
        if (requirement.getCuisineType() != null) {
            if (dish.getDescription().contains(requirement.getCuisineType()) || 
                dish.getName().contains(requirement.getCuisineType())) {
                score += 25;
            }
        }
        
        return Math.min(100.0, score);
    }
    
    /**
     * 使用大模型优化推荐理由
     */
    private List<DishRecommendation> enhanceWithLLM(List<DishRecommendation> recommendations, 
                                                   UserRequirement requirement, AgentContext context) {
        for (DishRecommendation rec : recommendations) {
            String reasonPrompt = String.format("""
                为以下菜品生成个性化推荐理由：
                
                菜品：%s
                价格：%.2f元
                描述：%s
                
                用户需求：
                - 用餐人数：%s人
                - 口味偏好：%s
                - 菜系偏好：%s
                - 预算：%s元/人
                
                请生成一句简洁的推荐理由（30字以内），说明为什么推荐这道菜：
                """, 
                rec.getDish().getName(),
                rec.getDish().getPrice(),
                rec.getDish().getDescription(),
                requirement.getPeopleCount(),
                requirement.getTastePreferences(),
                requirement.getCuisineType(),
                requirement.getBudgetRange()
            );
            
            try {
                String reason = chatClient.call(reasonPrompt);
                rec.setReason(reason.trim());
            } catch (Exception e) {
                log.warn("生成推荐理由失败: {}", e.getMessage());
                rec.setReason("符合您的口味偏好");
            }
        }
        
        return recommendations;
    }
    
    /**
     * 计算总价格
     */
    private BigDecimal calculateTotalPrice(List<DishRecommendation> recommendations) {
        return recommendations.stream()
                .map(rec -> rec.getDish().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * 计算置信度
     */
    private double calculateConfidence(List<DishRecommendation> recommendations) {
        if (recommendations.isEmpty()) {
            return 0.0;
        }
        
        double avgScore = recommendations.stream()
                .mapToDouble(DishRecommendation::getScore)
                .average()
                .orElse(0.0);
        
        return Math.min(100.0, avgScore);
    }
    
    @Override
    public AgentCapability getCapability() {
        return AgentCapability.builder()
                .name("RecommendationAgent")
                .description("基于RAG的智能推荐Agent，采用ReAct模式")
                .inputType("用户需求 (UserRequirement)")
                .outputType("推荐结果 (RecommendationResult)")
                .supportedFeatures(Arrays.asList("RAG检索", "内容过滤", "智能排序", "个性化推荐"))
                .version("1.0.0")
                .build();
    }
    
    @Override
    public int getPriority() {
        return 80; // 中等优先级，在理解之后执行
    }
} 