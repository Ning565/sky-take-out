package com.star.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DishRecommendation {
    private Long dishId; // 菜品ID
    private String dishName; // 菜品名称
    private BigDecimal price; // 价格
    private String description; // 描述
    private Double matchScore; // 匹配分数
    private String reason; // 推荐理由
} 