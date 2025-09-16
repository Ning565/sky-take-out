package com.star.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserRequirement {
    private Integer peopleCount; // 用餐人数
    private String diningPurpose; // 用餐目的
    private List<String> tastePreferences; // 口味偏好
    private String cuisineType; // 菜系偏好
    private Integer budgetRange; // 预算范围（人均）
    private List<String> dietaryRestrictions; // 饮食禁忌
    private String mealTime; // 用餐时间
    private List<String> specialNeeds; // 特殊需求
} 