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
 * 菜品匹配器
 * 负责菜品与用户需求的智能匹配，使用多种匹配算法提高推荐准确性。
 */
@Component
@Slf4j
public class DishMatcher {
    
    /**
     * 匹配算法类型
     */
    public enum MatchingAlgorithm {
        /**
         * 基于关键词匹配
         */
        KEYWORD_BASED,
        
        /**
         * 基于特征向量匹配（简化版）
         */
        FEATURE_BASED,
        
        /**
         * 基于规则的匹配
         */
        RULE_BASED,
        
        /**
         * 混合匹配算法
         */
        HYBRID
    }
    
    /**
     * 菜品特征权重配置
     */
    private static final Map<String, Double> FEATURE_WEIGHTS = new HashMap<>();
    
    static {
        FEATURE_WEIGHTS.put("name_match", 0.30);        // 名称匹配权重
        FEATURE_WEIGHTS.put("description_match", 0.25); // 描述匹配权重
        FEATURE_WEIGHTS.put("price_match", 0.20);       // 价格匹配权重
        FEATURE_WEIGHTS.put("category_match", 0.15);    // 分类匹配权重
        FEATURE_WEIGHTS.put("semantic_match", 0.10);    // 语义匹配权重
    }
    
    /**
     * 执行智能匹配
     * 
     * @param dishes 候选菜品列表
     * @param requirement 用户需求
     * @param algorithm 匹配算法类型
     * @return 匹配结果列表
     */
    public List<DishRecommendation> performMatching(List<Dish> dishes, UserRequirement requirement, 
                                                   MatchingAlgorithm algorithm) {
        log.info("开始菜品匹配，候选菜品数量：{}，算法类型：{}", dishes.size(), algorithm);
        
        List<DishRecommendation> matchResults = new ArrayList<>();
        
        for (Dish dish : dishes) {
            double matchScore = calculateMatchScore(dish, requirement, algorithm);
            
            if (matchScore > 0.1) { // 过滤掉匹配度过低的菜品
                DishRecommendation recommendation = createMatchResult(dish, matchScore, requirement, algorithm);
                matchResults.add(recommendation);
            }
        }
        
        // 按匹配度排序
        List<DishRecommendation> sortedResults = matchResults.stream()
                .sorted((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()))
                .collect(Collectors.toList());
        
        log.info("菜品匹配完成，匹配结果数量：{}", sortedResults.size());
        return sortedResults;
    }
    
    /**
     * 批量匹配多个用户需求
     * 
     * @param dishes 菜品列表
     * @param requirements 多个用户需求
     * @return 匹配结果映射
     */
    public Map<UserRequirement, List<DishRecommendation>> batchMatching(List<Dish> dishes, 
                                                                       List<UserRequirement> requirements) {
        log.info("开始批量匹配，菜品数量：{}，需求数量：{}", dishes.size(), requirements.size());
        
        Map<UserRequirement, List<DishRecommendation>> batchResults = new HashMap<>();
        
        for (UserRequirement requirement : requirements) {
            List<DishRecommendation> matches = performMatching(dishes, requirement, MatchingAlgorithm.HYBRID);
            batchResults.put(requirement, matches);
        }
        
        return batchResults;
    }
    
    /**
     * 相似菜品推荐
     * 基于已选菜品推荐相似的菜品
     */
    public List<DishRecommendation> findSimilarDishes(Dish referenceDish, List<Dish> candidateDishes, 
                                                     int maxResults) {
        log.info("查找相似菜品，参考菜品：{}，候选数量：{}", referenceDish.getName(), candidateDishes.size());
        
        List<DishRecommendation> similarDishes = new ArrayList<>();
        
        for (Dish candidate : candidateDishes) {
            if (candidate.getId().equals(referenceDish.getId())) {
                continue; // 跳过自己
            }
            
            double similarity = calculateDishSimilarity(referenceDish, candidate);
            
            if (similarity > 0.3) { // 相似度阈值
                DishRecommendation recommendation = new DishRecommendation();
                recommendation.setDish(candidate);
                recommendation.setMatchScore(similarity);
                recommendation.setReason("与" + referenceDish.getName() + "相似");
                similarDishes.add(recommendation);
            }
        }
        
        return similarDishes.stream()
                .sorted((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()))
                .limit(maxResults)
                .collect(Collectors.toList());
    }
    
    /**
     * 互补菜品推荐
     * 推荐与已选菜品互补的菜品（营养、口味等方面）
     */
    public List<DishRecommendation> findComplementaryDishes(List<Dish> selectedDishes, 
                                                           List<Dish> candidateDishes, 
                                                           UserRequirement requirement) {
        log.info("查找互补菜品，已选菜品数量：{}，候选数量：{}", selectedDishes.size(), candidateDishes.size());
        
        // 分析已选菜品的特征
        DishProfile selectedProfile = analyzeDishProfile(selectedDishes);
        
        List<DishRecommendation> complementaryDishes = new ArrayList<>();
        
        for (Dish candidate : candidateDishes) {
            // 检查是否已被选中
            if (selectedDishes.stream().anyMatch(d -> d.getId().equals(candidate.getId()))) {
                continue;
            }
            
            double complementScore = calculateComplementScore(candidate, selectedProfile, requirement);
            
            if (complementScore > 0.4) {
                DishRecommendation recommendation = new DishRecommendation();
                recommendation.setDish(candidate);
                recommendation.setMatchScore(complementScore);
                recommendation.setReason(generateComplementReason(candidate, selectedProfile));
                complementaryDishes.add(recommendation);
            }
        }
        
        return complementaryDishes.stream()
                .sorted((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()))
                .collect(Collectors.toList());
    }
    
    /**
     * 动态权重调整
     * 根据用户反馈动态调整匹配权重
     */
    public void adjustWeights(Map<String, Double> feedbackWeights) {
        log.info("调整匹配权重，反馈权重：{}", feedbackWeights);
        
        for (Map.Entry<String, Double> entry : feedbackWeights.entrySet()) {
            String feature = entry.getKey();
            Double adjustment = entry.getValue();
            
            if (FEATURE_WEIGHTS.containsKey(feature)) {
                double currentWeight = FEATURE_WEIGHTS.get(feature);
                double newWeight = Math.max(0.01, Math.min(0.99, currentWeight + adjustment));
                FEATURE_WEIGHTS.put(feature, newWeight);
                
                log.debug("权重调整：{} {} -> {}", feature, currentWeight, newWeight);
            }
        }
        
        // 权重归一化
        normalizeWeights();
    }
    
    // ==================== 私有匹配算法实现 ====================
    
    /**
     * 计算匹配分数
     */
    private double calculateMatchScore(Dish dish, UserRequirement requirement, MatchingAlgorithm algorithm) {
        switch (algorithm) {
            case KEYWORD_BASED:
                return keywordBasedMatching(dish, requirement);
            case FEATURE_BASED:
                return featureBasedMatching(dish, requirement);
            case RULE_BASED:
                return ruleBasedMatching(dish, requirement);
            case HYBRID:
                return hybridMatching(dish, requirement);
            default:
                return hybridMatching(dish, requirement);
        }
    }
    
    /**
     * 基于关键词的匹配
     */
    private double keywordBasedMatching(Dish dish, UserRequirement requirement) {
        double score = 0.0;
        int totalKeywords = 0;
        int matchedKeywords = 0;
        
        String dishContent = (dish.getName() + " " + dish.getDescription()).toLowerCase();
        
        // 检查菜系关键词
        if (requirement.getCuisineType() != null) {
            totalKeywords++;
            if (dishContent.contains(requirement.getCuisineType().toLowerCase())) {
                matchedKeywords++;
                score += 0.3;
            }
        }
        
        // 检查口味关键词
        if (requirement.getTastePreferences() != null) {
            for (String taste : requirement.getTastePreferences()) {
                totalKeywords++;
                if (dishContent.contains(taste.toLowerCase())) {
                    matchedKeywords++;
                    score += 0.2;
                }
            }
        }
        
        // 检查用餐目的关键词
        if (requirement.getDiningPurpose() != null) {
            totalKeywords++;
            String purpose = requirement.getDiningPurpose().toLowerCase();
            if ((purpose.contains("商务") && dishContent.contains("精致")) ||
                (purpose.contains("家庭") && dishContent.contains("家常")) ||
                (purpose.contains("约会") && dishContent.contains("浪漫"))) {
                matchedKeywords++;
                score += 0.15;
            }
        }
        
        // 计算匹配率加权
        if (totalKeywords > 0) {
            double matchRate = (double) matchedKeywords / totalKeywords;
            score = score * 0.7 + matchRate * 0.3;
        }
        
        return Math.min(1.0, score);
    }
    
    /**
     * 基于特征向量的匹配（简化版）
     */
    private double featureBasedMatching(Dish dish, UserRequirement requirement) {
        // 构建菜品特征向量
        double[] dishVector = buildDishFeatureVector(dish);
        
        // 构建需求特征向量
        double[] requirementVector = buildRequirementFeatureVector(requirement);
        
        // 计算余弦相似度
        return calculateCosineSimilarity(dishVector, requirementVector);
    }
    
    /**
     * 基于规则的匹配
     */
    private double ruleBasedMatching(Dish dish, UserRequirement requirement) {
        double score = 0.0;
        List<String> appliedRules = new ArrayList<>();
        
        // 规则1：价格匹配
        if (requirement.getBudgetRange() != null) {
            double dishPrice = dish.getPrice().doubleValue();
            double budget = requirement.getBudgetRange();
            
            if (dishPrice <= budget) {
                score += 0.25;
                appliedRules.add("价格匹配");
            } else if (dishPrice <= budget * 1.2) {
                score += 0.15;
                appliedRules.add("价格可接受");
            }
        }
        
        // 规则2：菜系匹配
        if (requirement.getCuisineType() != null) {
            String dishContent = (dish.getName() + " " + dish.getDescription()).toLowerCase();
            if (dishContent.contains(requirement.getCuisineType().toLowerCase())) {
                score += 0.30;
                appliedRules.add("菜系匹配");
            }
        }
        
        // 规则3：口味匹配
        if (requirement.getTastePreferences() != null && !requirement.getTastePreferences().isEmpty()) {
            String dishContent = (dish.getName() + " " + dish.getDescription()).toLowerCase();
            int tasteMatches = 0;
            
            for (String taste : requirement.getTastePreferences()) {
                if (dishContent.contains(taste.toLowerCase())) {
                    tasteMatches++;
                }
            }
            
            if (tasteMatches > 0) {
                score += 0.25 * ((double) tasteMatches / requirement.getTastePreferences().size());
                appliedRules.add("口味匹配");
            }
        }
        
        // 规则4：营养均衡
        String nutritionType = getNutritionCategory(dish);
        if (isNutritionNeeded(nutritionType, requirement)) {
            score += 0.15;
            appliedRules.add("营养均衡");
        }
        
        // 规则5：饮食禁忌检查（负分）
        if (requirement.getDietaryRestrictions() != null) {
            String dishContent = (dish.getName() + " " + dish.getDescription()).toLowerCase();
            for (String restriction : requirement.getDietaryRestrictions()) {
                if (dishContent.contains(restriction.toLowerCase())) {
                    score -= 0.5; // 严重扣分
                    appliedRules.add("违反饮食禁忌");
                    break;
                }
            }
        }
        
        log.debug("规则匹配 - 菜品：{}，分数：{}，应用规则：{}", dish.getName(), score, appliedRules);
        return Math.max(0.0, Math.min(1.0, score));
    }
    
    /**
     * 混合匹配算法
     */
    private double hybridMatching(Dish dish, UserRequirement requirement) {
        double keywordScore = keywordBasedMatching(dish, requirement);
        double featureScore = featureBasedMatching(dish, requirement);
        double ruleScore = ruleBasedMatching(dish, requirement);
        
        // 加权组合
        double hybridScore = keywordScore * 0.4 + featureScore * 0.3 + ruleScore * 0.3;
        
        log.debug("混合匹配 - 菜品：{}，关键词：{}，特征：{}，规则：{}，混合：{}", 
                dish.getName(), keywordScore, featureScore, ruleScore, hybridScore);
        
        return hybridScore;
    }
    
    /**
     * 计算菜品相似度
     */
    private double calculateDishSimilarity(Dish dish1, Dish dish2) {
        double nameSimilarity = calculateTextSimilarity(dish1.getName(), dish2.getName());
        double descSimilarity = calculateTextSimilarity(dish1.getDescription(), dish2.getDescription());
        double priceSimilarity = calculatePriceSimilarity(dish1.getPrice(), dish2.getPrice());
        
        return nameSimilarity * 0.4 + descSimilarity * 0.4 + priceSimilarity * 0.2;
    }
    
    // ==================== 辅助工具方法 ====================
    
    private double[] buildDishFeatureVector(Dish dish) {
        // 简化的特征向量：[价格档次, 辣度, 甜度, 营养价值, 受欢迎程度]
        String content = (dish.getName() + " " + dish.getDescription()).toLowerCase();
        
        return new double[]{
            normalizePriceLevel(dish.getPrice()),                    // 价格档次
            content.contains("辣") ? 1.0 : 0.0,                     // 辣度
            content.contains("甜") ? 1.0 : 0.0,                     // 甜度
            content.contains("营养") || content.contains("健康") ? 1.0 : 0.5, // 营养价值
            0.7 // 假设受欢迎程度（实际应该从数据库获取）
        };
    }
    
    private double[] buildRequirementFeatureVector(UserRequirement requirement) {
        // 构建需求特征向量
        double pricePreference = requirement.getBudgetRange() != null ? 
                normalizePriceLevel(BigDecimal.valueOf(requirement.getBudgetRange())) : 0.5;
        
        double spicyPreference = requirement.getTastePreferences() != null && 
                requirement.getTastePreferences().contains("辣") ? 1.0 : 0.0;
        
        double sweetPreference = requirement.getTastePreferences() != null && 
                requirement.getTastePreferences().contains("甜") ? 1.0 : 0.0;
        
        return new double[]{pricePreference, spicyPreference, sweetPreference, 0.7, 0.8};
    }
    
    private double calculateCosineSimilarity(double[] vector1, double[] vector2) {
        if (vector1.length != vector2.length) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += Math.pow(vector1[i], 2);
            norm2 += Math.pow(vector2[i], 2);
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    private double normalizePriceLevel(BigDecimal price) {
        double priceValue = price.doubleValue();
        if (priceValue <= 20) return 0.2;      // 便宜
        else if (priceValue <= 50) return 0.4; // 中等偏低
        else if (priceValue <= 80) return 0.6; // 中等
        else if (priceValue <= 120) return 0.8; // 中等偏高
        else return 1.0;                        // 昂贵
    }
    
    private double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) return 0.0;
        
        String[] words1 = text1.toLowerCase().split("\\s+");
        String[] words2 = text2.toLowerCase().split("\\s+");
        
        Set<String> set1 = new HashSet<>(Arrays.asList(words1));
        Set<String> set2 = new HashSet<>(Arrays.asList(words2));
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    private double calculatePriceSimilarity(BigDecimal price1, BigDecimal price2) {
        double diff = Math.abs(price1.doubleValue() - price2.doubleValue());
        double avgPrice = (price1.doubleValue() + price2.doubleValue()) / 2;
        
        if (avgPrice == 0) return 1.0;
        
        double relativeDiff = diff / avgPrice;
        return Math.max(0.0, 1.0 - relativeDiff);
    }
    
    private DishRecommendation createMatchResult(Dish dish, double matchScore, UserRequirement requirement, 
                                               MatchingAlgorithm algorithm) {
        DishRecommendation recommendation = new DishRecommendation();
        recommendation.setDish(dish);
        recommendation.setMatchScore(matchScore);
        recommendation.setReason(generateMatchReason(dish, requirement, algorithm, matchScore));
        return recommendation;
    }
    
    private String generateMatchReason(Dish dish, UserRequirement requirement, MatchingAlgorithm algorithm, 
                                     double matchScore) {
        List<String> reasons = new ArrayList<>();
        
        if (matchScore > 0.8) {
            reasons.add("高度匹配您的需求");
        } else if (matchScore > 0.6) {
            reasons.add("较好匹配您的偏好");
        } else {
            reasons.add("基本符合要求");
        }
        
        // 添加具体匹配原因
        if (requirement.getCuisineType() != null) {
            String content = (dish.getName() + " " + dish.getDescription()).toLowerCase();
            if (content.contains(requirement.getCuisineType().toLowerCase())) {
                reasons.add("符合" + requirement.getCuisineType() + "偏好");
            }
        }
        
        return String.join("，", reasons);
    }
    
    private String getNutritionCategory(Dish dish) {
        String content = (dish.getName() + " " + dish.getDescription()).toLowerCase();
        if (content.contains("肉") || content.contains("鸡") || content.contains("鱼")) return "蛋白质";
        if (content.contains("菜") || content.contains("豆")) return "维生素";
        if (content.contains("饭") || content.contains("面")) return "碳水化合物";
        return "其他";
    }
    
    private boolean isNutritionNeeded(String nutritionType, UserRequirement requirement) {
        // 简化的营养需求判断逻辑
        return true; // 暂时返回true，实际应该根据已选菜品判断
    }
    
    private DishProfile analyzeDishProfile(List<Dish> dishes) {
        // 分析菜品组合的整体特征
        DishProfile profile = new DishProfile();
        
        for (Dish dish : dishes) {
            String content = (dish.getName() + " " + dish.getDescription()).toLowerCase();
            
            if (content.contains("辣")) profile.spicyCount++;
            if (content.contains("甜")) profile.sweetCount++;
            if (content.contains("肉")) profile.meatCount++;
            if (content.contains("菜")) profile.vegetableCount++;
            
            profile.totalPrice = profile.totalPrice.add(dish.getPrice());
        }
        
        return profile;
    }
    
    private double calculateComplementScore(Dish candidate, DishProfile selectedProfile, UserRequirement requirement) {
        double score = 0.5; // 基础分
        String content = (candidate.getName() + " " + candidate.getDescription()).toLowerCase();
        
        // 口味互补
        if (selectedProfile.spicyCount > 0 && content.contains("清淡")) {
            score += 0.2;
        }
        
        // 营养互补
        if (selectedProfile.meatCount > 0 && content.contains("菜")) {
            score += 0.2;
        }
        
        // 价格均衡
        if (requirement.getBudgetRange() != null) {
            double avgSelectedPrice = selectedProfile.totalPrice.doubleValue() / Math.max(1, selectedProfile.dishCount);
            double candidatePrice = candidate.getPrice().doubleValue();
            
            if (Math.abs(candidatePrice - avgSelectedPrice) / avgSelectedPrice < 0.3) {
                score += 0.1;
            }
        }
        
        return Math.min(1.0, score);
    }
    
    private String generateComplementReason(Dish candidate, DishProfile selectedProfile) {
        List<String> reasons = new ArrayList<>();
        String content = (candidate.getName() + " " + candidate.getDescription()).toLowerCase();
        
        if (selectedProfile.spicyCount > 0 && content.contains("清淡")) {
            reasons.add("平衡口味");
        }
        
        if (selectedProfile.meatCount > 0 && content.contains("菜")) {
            reasons.add("营养均衡");
        }
        
        if (reasons.isEmpty()) {
            reasons.add("搭配推荐");
        }
        
        return String.join("，", reasons);
    }
    
    private void normalizeWeights() {
        double sum = FEATURE_WEIGHTS.values().stream().mapToDouble(Double::doubleValue).sum();
        
        if (sum > 0) {
            for (Map.Entry<String, Double> entry : FEATURE_WEIGHTS.entrySet()) {
                entry.setValue(entry.getValue() / sum);
            }
        }
    }
    
    /**
     * 菜品组合特征描述类
     */
    private static class DishProfile {
        int spicyCount = 0;
        int sweetCount = 0;
        int meatCount = 0;
        int vegetableCount = 0;
        int dishCount = 0;
        BigDecimal totalPrice = BigDecimal.ZERO;
    }
}
