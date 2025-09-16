package com.star.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 菜谱推荐及营养分析结果
 */
@Data
public class RecipeNutritionResult {
    private List<DishRecommendation> dishes; // 推荐菜品列表
    private BigDecimal totalPrice; // 总价格
    private Double confidenceScore; // 推荐置信度
    private Map<String, Object> nutritionInfo; // 营养分析信息（如热量、蛋白质、脂肪等）
} 