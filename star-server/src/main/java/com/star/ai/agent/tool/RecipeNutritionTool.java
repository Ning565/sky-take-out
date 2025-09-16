package com.star.ai.agent.tool;

import com.star.dto.UserRequirement;
import com.star.vo.DishRecommendation;
import com.star.vo.RecipeNutritionResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 菜谱/营养分析Tool
 * 支持结构化参数输入，返回结构化推荐和营养分析结果
 * 参考OpenAI function-calling风格，便于大模型/Agent自动调用
 */
@Component
public class RecipeNutritionTool implements Tool {
    @Override
    public String getName() {
        return "RecipeNutritionTool";
    }

    @Override
    public String getDescription() {
        return "根据用户结构化需求推荐菜品并分析营养，支持人群、口味、预算等多维度定制。";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        // 参考OpenAI function-calling参数定义
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        properties.put("peopleCount", Map.of("type", "integer", "description", "用餐人数"));
        properties.put("diningPurpose", Map.of("type", "string", "description", "用餐目的"));
        properties.put("tastePreferences", Map.of("type", "array", "items", Map.of("type", "string"), "description", "口味偏好"));
        properties.put("cuisineType", Map.of("type", "string", "description", "菜系偏好"));
        properties.put("budgetRange", Map.of("type", "integer", "description", "预算范围（人均）"));
        properties.put("dietaryRestrictions", Map.of("type", "array", "items", Map.of("type", "string"), "description", "饮食禁忌"));
        properties.put("mealTime", Map.of("type", "string", "description", "用餐时间"));
        properties.put("specialNeeds", Map.of("type", "array", "items", Map.of("type", "string"), "description", "特殊需求"));
        schema.put("properties", properties);
        schema.put("required", List.of("peopleCount"));
        schema.put("additionalProperties", false);
        return schema;
    }

    @Override
    public Object call(Map<String, Object> params) {
        // 参数转UserRequirement
        UserRequirement req = new UserRequirement();
        if (params.get("peopleCount") != null) req.setPeopleCount((Integer) params.get("peopleCount"));
        if (params.get("diningPurpose") != null) req.setDiningPurpose((String) params.get("diningPurpose"));
        if (params.get("tastePreferences") != null) req.setTastePreferences((List<String>) params.get("tastePreferences"));
        if (params.get("cuisineType") != null) req.setCuisineType((String) params.get("cuisineType"));
        if (params.get("budgetRange") != null) req.setBudgetRange((Integer) params.get("budgetRange"));
        if (params.get("dietaryRestrictions") != null) req.setDietaryRestrictions((List<String>) params.get("dietaryRestrictions"));
        if (params.get("mealTime") != null) req.setMealTime((String) params.get("mealTime"));
        if (params.get("specialNeeds") != null) req.setSpecialNeeds((List<String>) params.get("specialNeeds"));
        return queryRecipeAndNutrition(req);
    }

    /**
     * 查询菜品推荐及营养分析
     * @param req 用户结构化需求
     * @return 推荐及营养分析结果（Mock实现）
     */
    public RecipeNutritionResult queryRecipeAndNutrition(UserRequirement req) {
        // Mock推荐菜品
        List<DishRecommendation> dishes = new ArrayList<>();
        dishes.add(mockDish(1001L, "麻婆豆腐", new BigDecimal("28.00"), "经典川菜，豆腐嫩滑，麻辣鲜香", 0.95, "符合您的辣味偏好，适合" + req.getPeopleCount() + "人份"));
        dishes.add(mockDish(1002L, "宫保鸡丁", new BigDecimal("32.00"), "川菜名菜，鸡丁鲜嫩，微辣微甜", 0.92, "经典川菜，适合大众口味"));
        dishes.add(mockDish(1003L, "水煮鱼片", new BigDecimal("58.00"), "鱼片鲜嫩，麻辣爽口", 0.90, "高蛋白，适合喜欢辣味的朋友"));

        // Mock营养分析
        Map<String, Object> nutritionInfo = new HashMap<>();
        nutritionInfo.put("总热量", 1200);
        nutritionInfo.put("蛋白质", "80g");
        nutritionInfo.put("脂肪", "60g");
        nutritionInfo.put("碳水化合物", "100g");
        nutritionInfo.put("膳食纤维", "10g");
        nutritionInfo.put("钠", "2000mg");

        // 构建结果
        RecipeNutritionResult result = new RecipeNutritionResult();
        result.setDishes(dishes);
        result.setTotalPrice(dishes.stream().map(DishRecommendation::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add));
        result.setConfidenceScore(0.93);
        result.setNutritionInfo(nutritionInfo);
        return result;
    }

    private DishRecommendation mockDish(Long id, String name, BigDecimal price, String desc, double score, String reason) {
        DishRecommendation dish = new DishRecommendation();
        dish.setDishId(id);
        dish.setDishName(name);
        dish.setPrice(price);
        dish.setDescription(desc);
        dish.setMatchScore(score);
        dish.setReason(reason);
        return dish;
    }
} 