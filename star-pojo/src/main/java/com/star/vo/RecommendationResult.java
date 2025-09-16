package com.star.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class RecommendationResult {
    private List<DishRecommendation> dishes; // 推荐菜品列表
    private BigDecimal totalPrice; // 总价格
    private Double confidenceScore; // 推荐置信度
} 