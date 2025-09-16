package com.star.ai.recommendation;

import com.star.pojo.entity.Dish;
import com.star.pojo.dto.UserRequirement;
import com.star.pojo.vo.DishRecommendation;
import com.star.pojo.vo.RecommendationResult;
import com.star.ai.recommendation.DishMatcher.MatchingAlgorithm;
import com.star.mapper.DishMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 推荐引擎
 * 负责智能推荐的核心逻辑，整合多种推荐算法，提供完整的推荐服务。
 */
@Component
@Slf4j
public class RecommendationEngine {
    
    @Autowired
    private DishMapper dishMapper;
    
    @Autowired
    private DishMatcher dishMatcher;
    
    @Autowired
    private ContentBasedFilter contentBasedFilter;
    
    /**
     * 推荐策略枚举
     */
    public enum RecommendationStrategy {
        /**
         * 基础推荐（仅基于用户需求）
         */
        BASIC,
        
        /**
         * 个性化推荐（基于用户历史偏好）
         */
        PERSONALIZED,
        
        /**
         * 协同过滤推荐（基于相似用户）
         */
        COLLABORATIVE,
        
        /**
         * 混合推荐（综合多种策略）
         */
        HYBRID,
        
        /**
         * 实时推荐（考虑当前上下文）
         */
        REAL_TIME
    }
    
    /**
     * 推荐配置
     */
    private static final int DEFAULT_RECOMMENDATION_COUNT = 8;
    private static final double MIN_RECOMMENDATION_SCORE = 0.3;
    private static final int MAX_PARALLEL_TASKS = 4;
    
    private final Executor asyncExecutor = Executors.newFixedThreadPool(MAX_PARALLEL_TASKS);
    
    /**
     * 生成推荐结果 - 核心方法
     * 
     * @param requirement 用户需求
     * @param strategy 推荐策略
     * @param userPreferences 用户偏好（可选）
     * @return 推荐结果
     */
    public RecommendationResult generateRecommendations(UserRequirement requirement, 
                                                       RecommendationStrategy strategy,
                                                       Map<String, Object> userPreferences) {
        log.info("开始生成推荐，策略：{}，用户需求：{}", strategy, requirement);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 获取候选菜品
            List<Dish> candidateDishes = getCandidateDishes(requirement);
            
            // 2. 根据策略生成推荐
            List<DishRecommendation> recommendations = executeRecommendationStrategy(
                    candidateDishes, requirement, strategy, userPreferences);
            
            // 3. 后处理和优化
            recommendations = postProcessRecommendations(recommendations, requirement);
            
            // 4. 构建最终结果
            RecommendationResult result = buildRecommendationResult(
                    recommendations, requirement, strategy, startTime);
            
            log.info("推荐生成完成，耗时：{}ms，推荐数量：{}", 
                    System.currentTimeMillis() - startTime, recommendations.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("推荐生成失败", e);
            return createErrorRecommendationResult(e.getMessage());
        }
    }
    
    /**
     * 批量生成推荐
     * 为多个用户需求同时生成推荐
     */
    public Map<UserRequirement, RecommendationResult> batchGenerateRecommendations(
            List<UserRequirement> requirements, RecommendationStrategy strategy) {
        
        log.info("开始批量推荐，需求数量：{}，策略：{}", requirements.size(), strategy);
        
        Map<UserRequirement, RecommendationResult> results = new HashMap<>();
        
        // 使用并行流提高性能
        Map<UserRequirement, CompletableFuture<RecommendationResult>> futures = requirements.stream()
                .collect(Collectors.toMap(
                        req -> req,
                        req -> CompletableFuture.supplyAsync(
                                () -> generateRecommendations(req, strategy, null), 
                                asyncExecutor)
                ));
        
        // 等待所有任务完成
        futures.forEach((req, future) -> {
            try {
                results.put(req, future.get());
            } catch (Exception e) {
                log.error("批量推荐中单个任务失败：{}", req, e);
                results.put(req, createErrorRecommendationResult(e.getMessage()));
            }
        });
        
        log.info("批量推荐完成，成功：{}，失败：{}", 
                results.values().stream().mapToLong(r -> r.getConfidenceScore() > 0 ? 1 : 0).sum(),
                results.values().stream().mapToLong(r -> r.getConfidenceScore() == 0 ? 1 : 0).sum());
        
        return results;
    }
    
    /**
     * 实时推荐更新
     * 基于用户实时行为更新推荐结果
     */
    public RecommendationResult updateRecommendationsRealTime(RecommendationResult currentResult,
                                                            String userAction,
                                                            Object actionContext) {
        log.info("实时更新推荐，用户行为：{}", userAction);
        
        List<DishRecommendation> updatedRecommendations = new ArrayList<>(currentResult.getDishes());
        
        switch (userAction.toLowerCase()) {
            case "like":
                // 用户点赞某个推荐，提升相似菜品的权重
                enhanceSimilarRecommendations(updatedRecommendations, actionContext);
                break;
                
            case "dislike":
                // 用户不喜欢某个推荐，降低相似菜品的权重
                suppressSimilarRecommendations(updatedRecommendations, actionContext);
                break;
                
            case "add_to_cart":
                // 用户加入购物车，推荐互补菜品
                addComplementaryRecommendations(updatedRecommendations, actionContext);
                break;
                
            case "view_detail":
                // 用户查看详情，提升该菜品相关推荐
                boostRelatedRecommendations(updatedRecommendations, actionContext);
                break;
                
            default:
                log.debug("未知用户行为，跳过更新：{}", userAction);
                return currentResult;
        }
        
        // 重新排序
        updatedRecommendations.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        // 构建更新后的结果
        RecommendationResult updatedResult = RecommendationResult.builder()
                .dishes(updatedRecommendations)
                .totalPrice(calculateTotalPrice(updatedRecommendations))
                .confidenceScore(recalculateConfidenceScore(updatedRecommendations))
                .searchQuery(currentResult.getSearchQuery())
                .reasoning("基于用户行为实时调整")
                .build();
        
        log.info("实时推荐更新完成");
        return updatedResult;
    }
    
    /**
     * 推荐解释生成
     * 为推荐结果生成可解释的理由
     */
    public List<String> generateRecommendationExplanations(RecommendationResult result, 
                                                          UserRequirement requirement) {
        List<String> explanations = new ArrayList<>();
        
        if (result.getDishes().isEmpty()) {
            explanations.add("暂时没有找到符合您需求的菜品推荐");
            return explanations;
        }
        
        // 总体推荐说明
        explanations.add(String.format("为您推荐了%d道菜品，总价%.2f元", 
                result.getDishes().size(), result.getTotalPrice()));
        
        if (requirement.getPeopleCount() != null) {
            BigDecimal avgPrice = result.getTotalPrice().divide(
                    BigDecimal.valueOf(requirement.getPeopleCount()), 2, BigDecimal.ROUND_HALF_UP);
            explanations.add(String.format("人均消费约%.2f元", avgPrice));
        }
        
        // 分析推荐特点
        Map<String, Long> tasteDistribution = analyzeTasteDistribution(result.getDishes());
        if (!tasteDistribution.isEmpty()) {
            String mainTaste = tasteDistribution.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("均衡");
            explanations.add(String.format("主要口味倾向：%s", mainTaste));
        }
        
        // 营养均衡分析
        if (isNutritionallyBalanced(result.getDishes())) {
            explanations.add("营养搭配均衡，包含蛋白质、维生素和碳水化合物");
        }
        
        // 价格合理性分析
        if (requirement.getBudgetRange() != null) {
            double avgDishPrice = result.getTotalPrice().doubleValue() / result.getDishes().size();
            if (avgDishPrice <= requirement.getBudgetRange()) {
                explanations.add("价格控制在预算范围内");
            } else {
                explanations.add("价格略超预算，但性价比较高");
            }
        }
        
        return explanations;
    }
    
    /**
     * 推荐性能评估
     */
    public Map<String, Object> evaluateRecommendationPerformance(RecommendationResult result,
                                                               UserRequirement requirement) {
        Map<String, Object> metrics = new HashMap<>();
        
        // 基础指标
        metrics.put("recommendation_count", result.getDishes().size());
        metrics.put("confidence_score", result.getConfidenceScore());
        metrics.put("total_price", result.getTotalPrice());
        
        // 多样性指标
        double diversity = calculateRecommendationDiversity(result.getDishes());
        metrics.put("diversity_score", diversity);
        
        // 覆盖度指标
        double coverage = calculateCategoryCoverage(result.getDishes());
        metrics.put("category_coverage", coverage);
        
        // 新颖度指标（简化实现）
        double novelty = calculateNoveltyScore(result.getDishes());
        metrics.put("novelty_score", novelty);
        
        // 匹配度指标
        double matchScore = result.getDishes().stream()
                .mapToDouble(DishRecommendation::getScore)
                .average()
                .orElse(0.0);
        metrics.put("average_match_score", matchScore);
        
        // 整体质量分数
        double qualityScore = (result.getConfidenceScore() * 0.4 + 
                              diversity * 0.2 + 
                              coverage * 0.2 + 
                              novelty * 0.1 + 
                              matchScore * 0.1);
        metrics.put("overall_quality_score", qualityScore);
        
        return metrics;
    }
    
    // ==================== 私有核心算法实现 ====================
    
    private List<Dish> getCandidateDishes(UserRequirement requirement) {
        try {
            // 根据需求构建查询条件
            Map<String, Object> queryParams = buildQueryParams(requirement);
            
            // 从数据库获取候选菜品
            List<Dish> candidates = dishMapper.selectByRequirement(queryParams);
            
            if (candidates.isEmpty()) {
                // 如果没有匹配结果，放宽条件重新查询
                log.warn("严格条件下无匹配结果，放宽查询条件");
                candidates = dishMapper.selectAll(); // 获取所有菜品作为候选
            }
            
            log.info("获取候选菜品数量：{}", candidates.size());
            return candidates;
            
        } catch (Exception e) {
            log.error("获取候选菜品失败", e);
            return dishMapper.selectAll(); // 异常情况下返回所有菜品
        }
    }
    
    private Map<String, Object> buildQueryParams(UserRequirement requirement) {
        Map<String, Object> params = new HashMap<>();
        
        // 价格范围
        if (requirement.getBudgetRange() != null) {
            params.put("maxPrice", requirement.getBudgetRange() * 1.5); // 允许超出预算50%
        }
        
        // 菜系类型
        if (requirement.getCuisineType() != null) {
            params.put("cuisineType", requirement.getCuisineType());
        }
        
        // 其他过滤条件...
        
        return params;
    }
    
    private List<DishRecommendation> executeRecommendationStrategy(List<Dish> candidates,
                                                                 UserRequirement requirement,
                                                                 RecommendationStrategy strategy,
                                                                 Map<String, Object> userPreferences) {
        switch (strategy) {
            case BASIC:
                return executeBasicRecommendation(candidates, requirement);
                
            case PERSONALIZED:
                return executePersonalizedRecommendation(candidates, requirement, userPreferences);
                
            case COLLABORATIVE:
                return executeCollaborativeRecommendation(candidates, requirement, userPreferences);
                
            case HYBRID:
                return executeHybridRecommendation(candidates, requirement, userPreferences);
                
            case REAL_TIME:
                return executeRealTimeRecommendation(candidates, requirement, userPreferences);
                
            default:
                return executeBasicRecommendation(candidates, requirement);
        }
    }
    
    private List<DishRecommendation> executeBasicRecommendation(List<Dish> candidates, 
                                                              UserRequirement requirement) {
        log.debug("执行基础推荐算法");
        
        // 使用混合匹配算法
        List<DishRecommendation> matchResults = dishMatcher.performMatching(
                candidates, requirement, MatchingAlgorithm.HYBRID);
        
        // 应用内容过滤
        List<DishRecommendation> filteredResults = contentBasedFilter.filterAndSort(
                matchResults.stream().map(DishRecommendation::getDish).collect(Collectors.toList()),
                requirement, DEFAULT_RECOMMENDATION_COUNT);
        
        return filteredResults.stream()
                .filter(rec -> rec.getScore() >= MIN_RECOMMENDATION_SCORE)
                .collect(Collectors.toList());
    }
    
    private List<DishRecommendation> executePersonalizedRecommendation(List<Dish> candidates,
                                                                     UserRequirement requirement,
                                                                     Map<String, Object> userPreferences) {
        log.debug("执行个性化推荐算法");
        
        // 先执行基础推荐
        List<DishRecommendation> basicResults = executeBasicRecommendation(candidates, requirement);
        
        // 应用个性化过滤
        if (userPreferences != null && !userPreferences.isEmpty()) {
            basicResults = contentBasedFilter.applyPersonalizedFilter(basicResults, userPreferences);
        }
        
        return basicResults;
    }
    
    private List<DishRecommendation> executeCollaborativeRecommendation(List<Dish> candidates,
                                                                       UserRequirement requirement,
                                                                       Map<String, Object> userPreferences) {
        log.debug("执行协同过滤推荐算法");
        
        // 简化的协同过滤实现
        // 实际项目中应该基于用户-物品评分矩阵实现
        List<DishRecommendation> personalizedResults = executePersonalizedRecommendation(
                candidates, requirement, userPreferences);
        
        // 基于相似用户的偏好调整权重
        return adjustByCollaborativeFiltering(personalizedResults, userPreferences);
    }
    
    private List<DishRecommendation> executeHybridRecommendation(List<Dish> candidates,
                                                               UserRequirement requirement,
                                                               Map<String, Object> userPreferences) {
        log.debug("执行混合推荐算法");
        
        // 并行执行多种算法
        CompletableFuture<List<DishRecommendation>> basicFuture = CompletableFuture.supplyAsync(
                () -> executeBasicRecommendation(candidates, requirement), asyncExecutor);
        
        CompletableFuture<List<DishRecommendation>> personalizedFuture = CompletableFuture.supplyAsync(
                () -> executePersonalizedRecommendation(candidates, requirement, userPreferences), asyncExecutor);
        
        CompletableFuture<List<DishRecommendation>> collaborativeFuture = CompletableFuture.supplyAsync(
                () -> executeCollaborativeRecommendation(candidates, requirement, userPreferences), asyncExecutor);
        
        try {
            List<DishRecommendation> basicResults = basicFuture.get();
            List<DishRecommendation> personalizedResults = personalizedFuture.get();
            List<DishRecommendation> collaborativeResults = collaborativeFuture.get();
            
            // 融合多种算法结果
            return fuseMultipleRecommendations(
                    Arrays.asList(basicResults, personalizedResults, collaborativeResults),
                    Arrays.asList(0.4, 0.4, 0.2)); // 权重分配
            
        } catch (Exception e) {
            log.error("混合推荐算法执行失败", e);
            return executeBasicRecommendation(candidates, requirement);
        }
    }
    
    private List<DishRecommendation> executeRealTimeRecommendation(List<Dish> candidates,
                                                                 UserRequirement requirement,
                                                                 Map<String, Object> userPreferences) {
        log.debug("执行实时推荐算法");
        
        // 获取实时上下文信息
        Map<String, Object> realtimeContext = getCurrentContext(userPreferences);
        
        // 基于实时上下文调整推荐
        List<DishRecommendation> hybridResults = executeHybridRecommendation(
                candidates, requirement, userPreferences);
        
        return adjustByRealtimeContext(hybridResults, realtimeContext);
    }
    
    private List<DishRecommendation> postProcessRecommendations(List<DishRecommendation> recommendations,
                                                              UserRequirement requirement) {
        log.debug("开始推荐结果后处理");
        
        // 1. 多样性过滤
        recommendations = contentBasedFilter.applyDiversityFilter(recommendations, 3.0);
        
        // 2. 营养均衡过滤
        recommendations = contentBasedFilter.applyNutritionBalanceFilter(recommendations, requirement);
        
        // 3. 最终排序
        recommendations.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        // 4. 限制数量
        if (recommendations.size() > DEFAULT_RECOMMENDATION_COUNT) {
            recommendations = recommendations.subList(0, DEFAULT_RECOMMENDATION_COUNT);
        }
        
        log.debug("推荐结果后处理完成，最终数量：{}", recommendations.size());
        return recommendations;
    }
    
    private RecommendationResult buildRecommendationResult(List<DishRecommendation> recommendations,
                                                         UserRequirement requirement,
                                                         RecommendationStrategy strategy,
                                                         long startTime) {
        return RecommendationResult.builder()
                .dishes(recommendations)
                .totalPrice(calculateTotalPrice(recommendations))
                .confidenceScore(calculateConfidenceScore(recommendations, requirement))
                .searchQuery(buildSearchQuery(requirement))
                .reasoning(String.format("使用%s策略生成推荐", strategy))
                .build();
    }
    
    // ==================== 辅助工具方法 ====================
    
    private BigDecimal calculateTotalPrice(List<DishRecommendation> recommendations) {
        return recommendations.stream()
                .map(rec -> rec.getDish().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private double calculateConfidenceScore(List<DishRecommendation> recommendations, 
                                          UserRequirement requirement) {
        if (recommendations.isEmpty()) {
            return 0.0;
        }
        
        double avgScore = recommendations.stream()
                .mapToDouble(DishRecommendation::getScore)
                .average()
                .orElse(0.0);
        
        // 根据推荐数量调整置信度
        double countFactor = Math.min(1.0, (double) recommendations.size() / DEFAULT_RECOMMENDATION_COUNT);
        
        return avgScore * countFactor;
    }
    
    private String buildSearchQuery(UserRequirement requirement) {
        List<String> queryParts = new ArrayList<>();
        
        if (requirement.getCuisineType() != null) {
            queryParts.add(requirement.getCuisineType());
        }
        
        if (requirement.getTastePreferences() != null && !requirement.getTastePreferences().isEmpty()) {
            queryParts.addAll(requirement.getTastePreferences());
        }
        
        if (requirement.getDiningPurpose() != null) {
            queryParts.add(requirement.getDiningPurpose());
        }
        
        return String.join(" ", queryParts);
    }
    
    private RecommendationResult createErrorRecommendationResult(String errorMessage) {
        return RecommendationResult.builder()
                .dishes(new ArrayList<>())
                .totalPrice(BigDecimal.ZERO)
                .confidenceScore(0.0)
                .searchQuery("")
                .reasoning("推荐生成失败: " + errorMessage)
                .build();
    }
    
    // 其他辅助方法的简化实现...
    private void enhanceSimilarRecommendations(List<DishRecommendation> recommendations, Object context) {
        // 实现相似推荐增强逻辑
    }
    
    private void suppressSimilarRecommendations(List<DishRecommendation> recommendations, Object context) {
        // 实现相似推荐抑制逻辑
    }
    
    private void addComplementaryRecommendations(List<DishRecommendation> recommendations, Object context) {
        // 实现互补推荐添加逻辑
    }
    
    private void boostRelatedRecommendations(List<DishRecommendation> recommendations, Object context) {
        // 实现相关推荐提升逻辑
    }
    
    private double recalculateConfidenceScore(List<DishRecommendation> recommendations) {
        return recommendations.stream()
                .mapToDouble(DishRecommendation::getScore)
                .average()
                .orElse(0.0);
    }
    
    private Map<String, Long> analyzeTasteDistribution(List<DishRecommendation> recommendations) {
        // 分析口味分布
        return new HashMap<>();
    }
    
    private boolean isNutritionallyBalanced(List<DishRecommendation> recommendations) {
        // 检查营养均衡性
        return true;
    }
    
    private double calculateRecommendationDiversity(List<DishRecommendation> recommendations) {
        // 计算推荐多样性
        return 0.8;
    }
    
    private double calculateCategoryCoverage(List<DishRecommendation> recommendations) {
        // 计算分类覆盖度
        return 0.7;
    }
    
    private double calculateNoveltyScore(List<DishRecommendation> recommendations) {
        // 计算新颖度分数
        return 0.6;
    }
    
    private List<DishRecommendation> adjustByCollaborativeFiltering(List<DishRecommendation> recommendations,
                                                                   Map<String, Object> userPreferences) {
        // 协同过滤调整
        return recommendations;
    }
    
    private List<DishRecommendation> fuseMultipleRecommendations(List<List<DishRecommendation>> resultLists,
                                                               List<Double> weights) {
        // 融合多个推荐结果
        Map<Long, DishRecommendation> fusedMap = new HashMap<>();
        
        for (int i = 0; i < resultLists.size(); i++) {
            List<DishRecommendation> results = resultLists.get(i);
            double weight = weights.get(i);
            
            for (DishRecommendation rec : results) {
                Long dishId = rec.getDish().getId();
                if (fusedMap.containsKey(dishId)) {
                    // 加权平均分数
                    DishRecommendation existing = fusedMap.get(dishId);
                    double newScore = existing.getScore() + rec.getScore() * weight;
                    existing.setScore(newScore);
                } else {
                    rec.setScore(rec.getScore() * weight);
                    fusedMap.put(dishId, rec);
                }
            }
        }
        
        return fusedMap.values().stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .collect(Collectors.toList());
    }
    
    private Map<String, Object> getCurrentContext(Map<String, Object> userPreferences) {
        Map<String, Object> context = new HashMap<>();
        context.put("currentTime", LocalDateTime.now());
        context.put("season", getCurrentSeason());
        // 添加其他实时上下文信息
        return context;
    }
    
    private String getCurrentSeason() {
        int month = LocalDateTime.now().getMonthValue();
        if (month >= 3 && month <= 5) return "春";
        else if (month >= 6 && month <= 8) return "夏";
        else if (month >= 9 && month <= 11) return "秋";
        else return "冬";
    }
    
    private List<DishRecommendation> adjustByRealtimeContext(List<DishRecommendation> recommendations,
                                                           Map<String, Object> context) {
        // 基于实时上下文调整推荐
        String season = (String) context.get("season");
        
        return recommendations.stream()
                .map(rec -> {
                    // 根据季节调整分数
                    if (isSeasonalDish(rec.getDish(), season)) {
                        rec.setScore(rec.getScore() * 1.1);
                    }
                    return rec;
                })
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .collect(Collectors.toList());
    }
    
    private boolean isSeasonalDish(Dish dish, String season) {
        String content = (dish.getName() + " " + dish.getDescription()).toLowerCase();
        switch (season) {
            case "夏": return content.contains("凉") || content.contains("冰") || content.contains("清爽");
            case "冬": return content.contains("热") || content.contains("暖") || content.contains("汤");
            default: return false;
        }
    }
} 