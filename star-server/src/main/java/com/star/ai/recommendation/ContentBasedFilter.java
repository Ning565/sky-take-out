package com.star.ai.recommendation;

import com.star.pojo.entity.Dish;
import com.star.pojo.dto.UserRequirement;
import com.star.pojo.vo.DishRecommendation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于内容的过滤器
 * 负责对推荐结果进行内容过滤和排序，实现个性化推荐算法。
 */
@Component
@Slf4j
public class ContentBasedFilter {
    
    /**
     * 过滤规则权重配置
     */
    private static final double PRICE_WEIGHT = 0.25;       // 价格匹配权重
    private static final double TASTE_WEIGHT = 0.30;       // 口味匹配权重
    private static final double CUISINE_WEIGHT = 0.20;     // 菜系匹配权重
    private static final double RESTRICTION_WEIGHT = 0.25; // 禁忌过滤权重
    
    /**
     * 过滤并排序菜品推荐
     * 
     * @param dishes 原始菜品列表
     * @param requirement 用户需求
     * @param maxResults 最大返回结果数
     * @return 过滤和排序后的推荐列表
     */
    public List<DishRecommendation> filterAndSort(List<Dish> dishes, UserRequirement requirement, int maxResults) {
        log.info("开始内容过滤，原始菜品数量：{}，最大结果数：{}", dishes.size(), maxResults);
        
        List<DishRecommendation> recommendations = new ArrayList<>();
        
        for (Dish dish : dishes) {
            // 1. 硬性过滤（饮食禁忌等）
            if (shouldHardFilter(dish, requirement)) {
                continue;
            }
            
            // 2. 计算匹配分数
            double matchScore = calculateMatchScore(dish, requirement);
            
            // 3. 创建推荐对象
            DishRecommendation recommendation = createRecommendation(dish, matchScore, requirement);
            recommendations.add(recommendation);
        }
        
        // 4. 排序和截取
        List<DishRecommendation> sortedRecommendations = recommendations.stream()
                .sorted((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()))
                .limit(maxResults)
                .collect(Collectors.toList());
        
        log.info("内容过滤完成，返回推荐数量：{}", sortedRecommendations.size());
        return sortedRecommendations;
    }
    
    /**
     * 应用个性化过滤
     * 基于用户历史偏好进行个性化调整
     */
    public List<DishRecommendation> applyPersonalizedFilter(List<DishRecommendation> recommendations, 
                                                           Map<String, Object> userPreferences) {
        if (userPreferences == null || userPreferences.isEmpty()) {
            return recommendations;
        }
        
        log.info("应用个性化过滤，用户偏好：{}", userPreferences);
        
        return recommendations.stream()
                .map(rec -> {
                    double personalizedScore = rec.getMatchScore() + calculatePersonalizationBonus(rec, userPreferences);
                    rec.setMatchScore(Math.min(1.0, personalizedScore)); // 限制最大分数为1.0
                    return rec;
                })
                .sorted((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()))
                .collect(Collectors.toList());
    }
    
    /**
     * 多样性过滤
     * 确保推荐结果的多样性，避免推荐过于相似的菜品
     */
    public List<DishRecommendation> applyDiversityFilter(List<DishRecommendation> recommendations, 
                                                        double diversityThreshold) {
        log.info("应用多样性过滤，多样性阈值：{}", diversityThreshold);
        
        List<DishRecommendation> diverseRecommendations = new ArrayList<>();
        Set<String> usedCategories = new HashSet<>();
        Set<String> usedTasteProfiles = new HashSet<>();
        
        for (DishRecommendation rec : recommendations) {
            String category = getCategoryName(rec.getDish());
            String tasteProfile = getTasteProfile(rec.getDish());
            
            // 检查多样性
            boolean isDiverse = true;
            
            // 如果同类菜品已经超过阈值，降低优先级
            if (usedCategories.contains(category) && usedCategories.size() >= diversityThreshold) {
                isDiverse = false;
            }
            
            if (usedTasteProfiles.contains(tasteProfile) && usedTasteProfiles.size() >= diversityThreshold) {
                isDiverse = false;
            }
            
            if (isDiverse || diverseRecommendations.size() < 3) { // 至少保证前3个推荐
                diverseRecommendations.add(rec);
                usedCategories.add(category);
                usedTasteProfiles.add(tasteProfile);
            }
        }
        
        log.info("多样性过滤完成，过滤后数量：{}", diverseRecommendations.size());
        return diverseRecommendations;
    }
    
    /**
     * 价格区间过滤
     */
    public List<DishRecommendation> filterByPriceRange(List<DishRecommendation> recommendations, 
                                                      BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice == null && maxPrice == null) {
            return recommendations;
        }
        
        return recommendations.stream()
                .filter(rec -> {
                    BigDecimal price = rec.getDish().getPrice();
                    boolean inRange = true;
                    
                    if (minPrice != null && price.compareTo(minPrice) < 0) {
                        inRange = false;
                    }
                    
                    if (maxPrice != null && price.compareTo(maxPrice) > 0) {
                        inRange = false;
                    }
                    
                    return inRange;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 营养均衡过滤
     * 确保推荐的菜品搭配营养均衡
     */
    public List<DishRecommendation> applyNutritionBalanceFilter(List<DishRecommendation> recommendations, 
                                                               UserRequirement requirement) {
        log.info("应用营养均衡过滤");
        
        // 营养类别统计
        Map<String, Integer> nutritionCounts = new HashMap<>();
        nutritionCounts.put("protein", 0);  // 蛋白质类
        nutritionCounts.put("vegetable", 0); // 蔬菜类
        nutritionCounts.put("carb", 0);     // 碳水化合物类
        
        List<DishRecommendation> balancedRecommendations = new ArrayList<>();
        
        // 按营养均衡原则选择菜品
        for (DishRecommendation rec : recommendations) {
            String nutritionType = getNutritionType(rec.getDish());
            int currentCount = nutritionCounts.getOrDefault(nutritionType, 0);
            
            // 如果某种营养类型已经足够，降低优先级
            if (currentCount < getMaxNutritionCount(nutritionType, requirement.getPeopleCount())) {
                rec.setMatchScore(rec.getMatchScore() + 0.1); // 营养均衡加分
                nutritionCounts.put(nutritionType, currentCount + 1);
            } else {
                rec.setMatchScore(rec.getMatchScore() - 0.05); // 营养过量减分
            }
            
            balancedRecommendations.add(rec);
        }
        
        // 重新排序
        return balancedRecommendations.stream()
                .sorted((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()))
                .collect(Collectors.toList());
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 硬性过滤检查
     */
    private boolean shouldHardFilter(Dish dish, UserRequirement requirement) {
        // 1. 检查饮食禁忌
        if (requirement.getDietaryRestrictions() != null) {
            for (String restriction : requirement.getDietaryRestrictions()) {
                if (containsRestriction(dish, restriction)) {
                    log.debug("菜品 {} 因饮食禁忌 {} 被过滤", dish.getName(), restriction);
                    return true;
                }
            }
        }
        
        // 2. 检查价格上限（硬性限制）
        if (requirement.getBudgetRange() != null) {
            double maxAcceptablePrice = requirement.getBudgetRange() * 2.0; // 允许超出预算100%
            if (dish.getPrice().doubleValue() > maxAcceptablePrice) {
                log.debug("菜品 {} 因价格过高被过滤：{} > {}", dish.getName(), dish.getPrice(), maxAcceptablePrice);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 计算匹配分数
     */
    private double calculateMatchScore(Dish dish, UserRequirement requirement) {
        double totalScore = 0.0;
        
        // 1. 价格匹配分数
        double priceScore = calculatePriceScore(dish, requirement);
        totalScore += priceScore * PRICE_WEIGHT;
        
        // 2. 口味匹配分数
        double tasteScore = calculateTasteScore(dish, requirement);
        totalScore += tasteScore * TASTE_WEIGHT;
        
        // 3. 菜系匹配分数
        double cuisineScore = calculateCuisineScore(dish, requirement);
        totalScore += cuisineScore * CUISINE_WEIGHT;
        
        // 4. 无禁忌加分
        double restrictionScore = calculateRestrictionScore(dish, requirement);
        totalScore += restrictionScore * RESTRICTION_WEIGHT;
        
        log.debug("菜品 {} 匹配分数：价格{}, 口味{}, 菜系{}, 禁忌{}, 总分{}", 
                dish.getName(), priceScore, tasteScore, cuisineScore, restrictionScore, totalScore);
        
        return Math.min(1.0, totalScore);
    }
    
    private double calculatePriceScore(Dish dish, UserRequirement requirement) {
        if (requirement.getBudgetRange() == null) {
            return 0.8; // 无预算限制时给中等分数
        }
        
        double dishPrice = dish.getPrice().doubleValue();
        double budget = requirement.getBudgetRange();
        
        if (dishPrice <= budget) {
            return 1.0; // 在预算内满分
        } else if (dishPrice <= budget * 1.2) {
            return 0.8; // 略超预算扣少量分
        } else if (dishPrice <= budget * 1.5) {
            return 0.5; // 超预算较多扣较多分
        } else {
            return 0.2; // 严重超预算低分
        }
    }
    
    private double calculateTasteScore(Dish dish, UserRequirement requirement) {
        if (requirement.getTastePreferences() == null || requirement.getTastePreferences().isEmpty()) {
            return 0.6; // 无口味偏好时给中等分数
        }
        
        double score = 0.0;
        int matchCount = 0;
        
        for (String taste : requirement.getTastePreferences()) {
            if (containsTaste(dish, taste)) {
                matchCount++;
            }
        }
        
        if (matchCount > 0) {
            score = (double) matchCount / requirement.getTastePreferences().size();
        }
        
        return Math.min(1.0, score + 0.3); // 至少给基础分
    }
    
    private double calculateCuisineScore(Dish dish, UserRequirement requirement) {
        if (requirement.getCuisineType() == null) {
            return 0.6; // 无菜系偏好时给中等分数
        }
        
        if (containsCuisine(dish, requirement.getCuisineType())) {
            return 1.0; // 完全匹配满分
        } else {
            return 0.3; // 不匹配给低分
        }
    }
    
    private double calculateRestrictionScore(Dish dish, UserRequirement requirement) {
        if (requirement.getDietaryRestrictions() == null || requirement.getDietaryRestrictions().isEmpty()) {
            return 1.0; // 无禁忌满分
        }
        
        // 如果包含任何禁忌成分，应该在硬性过滤阶段就被过滤掉
        // 能到这里说明不包含禁忌，给满分
        return 1.0;
    }
    
    private double calculatePersonalizationBonus(DishRecommendation recommendation, Map<String, Object> userPreferences) {
        double bonus = 0.0;
        
        // 基于历史点击偏好加分
        @SuppressWarnings("unchecked")
        List<String> preferredCategories = (List<String>) userPreferences.get("preferredCategories");
        if (preferredCategories != null) {
            String category = getCategoryName(recommendation.getDish());
            if (preferredCategories.contains(category)) {
                bonus += 0.1;
            }
        }
        
        // 基于历史口味偏好加分
        @SuppressWarnings("unchecked")
        List<String> preferredTastes = (List<String>) userPreferences.get("preferredTastes");
        if (preferredTastes != null) {
            for (String taste : preferredTastes) {
                if (containsTaste(recommendation.getDish(), taste)) {
                    bonus += 0.05;
                }
            }
        }
        
        return bonus;
    }
    
    private DishRecommendation createRecommendation(Dish dish, double matchScore, UserRequirement requirement) {
        DishRecommendation recommendation = new DishRecommendation();
        recommendation.setDish(dish);
        recommendation.setMatchScore(matchScore);
        recommendation.setReason(generateRecommendationReason(dish, requirement, matchScore));
        return recommendation;
    }
    
    private String generateRecommendationReason(Dish dish, UserRequirement requirement, double matchScore) {
        List<String> reasons = new ArrayList<>();
        
        if (requirement.getBudgetRange() != null && dish.getPrice().doubleValue() <= requirement.getBudgetRange()) {
            reasons.add("价格实惠");
        }
        
        if (requirement.getTastePreferences() != null) {
            for (String taste : requirement.getTastePreferences()) {
                if (containsTaste(dish, taste)) {
                    reasons.add("符合" + taste + "口味");
                    break;
                }
            }
        }
        
        if (requirement.getCuisineType() != null && containsCuisine(dish, requirement.getCuisineType())) {
            reasons.add("经典" + requirement.getCuisineType());
        }
        
        if (reasons.isEmpty()) {
            reasons.add("推荐菜品");
        }
        
        return String.join("，", reasons);
    }
    
    // 工具方法
    private boolean containsRestriction(Dish dish, String restriction) {
        String content = (dish.getName() + " " + dish.getDescription()).toLowerCase();
        return content.contains(restriction.toLowerCase());
    }
    
    private boolean containsTaste(Dish dish, String taste) {
        String content = (dish.getName() + " " + dish.getDescription()).toLowerCase();
        return content.contains(taste.toLowerCase());
    }
    
    private boolean containsCuisine(Dish dish, String cuisine) {
        String content = (dish.getName() + " " + dish.getDescription()).toLowerCase();
        return content.contains(cuisine.toLowerCase());
    }
    
    private String getCategoryName(Dish dish) {
        // 根据菜品ID或描述判断类别
        String desc = dish.getDescription() != null ? dish.getDescription().toLowerCase() : "";
        if (desc.contains("肉") || desc.contains("鸡") || desc.contains("鱼")) return "荤菜";
        if (desc.contains("蔬菜") || desc.contains("菌") || desc.contains("豆腐")) return "素菜";
        if (desc.contains("汤") || desc.contains("羹")) return "汤类";
        return "其他";
    }
    
    private String getTasteProfile(Dish dish) {
        String content = (dish.getName() + " " + dish.getDescription()).toLowerCase();
        if (content.contains("辣")) return "辣味";
        if (content.contains("甜")) return "甜味";
        if (content.contains("酸")) return "酸味";
        if (content.contains("清淡")) return "清淡";
        return "中性";
    }
    
    private String getNutritionType(Dish dish) {
        String content = (dish.getName() + " " + dish.getDescription()).toLowerCase();
        if (content.contains("肉") || content.contains("鸡") || content.contains("鱼") || content.contains("蛋")) {
            return "protein";
        }
        if (content.contains("菜") || content.contains("豆") || content.contains("菌")) {
            return "vegetable";
        }
        if (content.contains("饭") || content.contains("面") || content.contains("粥")) {
            return "carb";
        }
        return "other";
    }
    
    private int getMaxNutritionCount(String nutritionType, Integer peopleCount) {
        int baseCount = peopleCount != null ? peopleCount : 2;
        switch (nutritionType) {
            case "protein": return Math.max(1, baseCount / 2); // 蛋白质类菜品数量
            case "vegetable": return Math.max(1, baseCount / 2); // 蔬菜类菜品数量
            case "carb": return 1; // 主食类通常1道即可
            default: return baseCount;
        }
    }
} 