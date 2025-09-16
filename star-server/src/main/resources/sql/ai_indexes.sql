-- ===============================================
-- AI智能餐饮客服系统 - 索引优化脚本
-- 版本: 1.0
-- 创建时间: 2024-01-01
-- 说明: 针对AI查询场景优化数据库索引
-- ===============================================

-- 1. ai_conversation表索引优化
-- 会话查询优化
CREATE INDEX `idx_session_created` ON `ai_conversation` (`session_id`, `created_time`);

-- 用户对话历史查询优化
CREATE INDEX `idx_user_created` ON `ai_conversation` (`user_id`, `created_time`);

-- 对话状态统计优化
CREATE INDEX `idx_state_created` ON `ai_conversation` (`conversation_state`, `created_time`);

-- 响应时间性能分析
CREATE INDEX `idx_response_time` ON `ai_conversation` (`response_time`);

-- 2. dish_tags表索引优化
-- 菜品多标签查询优化（最常用）
CREATE INDEX `idx_dish_weight` ON `dish_tags` (`dish_id`, `weight` DESC);

-- 标签类型筛选优化
CREATE INDEX `idx_type_name_weight` ON `dish_tags` (`tag_type`, `tag_name`, `weight` DESC);

-- 标签置信度查询优化
CREATE INDEX `idx_confidence_source` ON `dish_tags` (`confidence` DESC, `source`);

-- 标签权重排序优化
CREATE INDEX `idx_weight_confidence` ON `dish_tags` (`weight` DESC, `confidence` DESC);

-- 3. ai_recommendation_log表索引优化
-- 推荐效果分析优化
CREATE INDEX `idx_score_feedback` ON `ai_recommendation_log` (`recommendation_score` DESC, `user_feedback`);

-- 用户推荐历史查询
CREATE INDEX `idx_user_created_score` ON `ai_recommendation_log` (`user_id`, `created_time`, `recommendation_score` DESC);

-- 算法效果对比分析
CREATE INDEX `idx_algorithm_score` ON `ai_recommendation_log` (`recommendation_algorithm`, `recommendation_score` DESC);

-- RAG效果分析
CREATE INDEX `idx_rag_score` ON `ai_recommendation_log` (`rag_enabled`, `recommendation_score` DESC);

-- 反馈统计分析
CREATE INDEX `idx_feedback_created` ON `ai_recommendation_log` (`user_feedback`, `created_time`);

-- 4. dish表AI相关字段索引优化
-- 辣度级别查询（常用筛选条件）
CREATE INDEX `idx_spice_price` ON `dish` (`spice_level`, `price`);

-- 制作难度筛选
CREATE INDEX `idx_difficulty_time` ON `dish` (`difficulty_level`, `cooking_time`);

-- AI推荐热度排序
CREATE INDEX `idx_recommend_order_count` ON `dish` (`recommend_count` DESC, `order_count_via_ai` DESC);

-- 向量更新状态查询
CREATE INDEX `idx_vector_updated` ON `dish` (`vector_updated_time`);

-- 价格区间+特征组合查询
CREATE INDEX `idx_price_spice_difficulty` ON `dish` (`price`, `spice_level`, `difficulty_level`);

-- 5. ai_model_stats表索引优化
-- 模型性能监控查询
CREATE INDEX `idx_model_date_type` ON `ai_model_stats` (`model_name`, `call_date`, `call_type`);

-- 日期范围统计查询
CREATE INDEX `idx_date_model` ON `ai_model_stats` (`call_date`, `model_name`);

-- 响应时间性能分析
CREATE INDEX `idx_avg_response_time` ON `ai_model_stats` (`avg_response_time` DESC);

-- 错误率统计
CREATE INDEX `idx_error_rate` ON `ai_model_stats` (`error_count` DESC, `call_count`);

-- 6. user_ai_preferences表索引优化
-- 用户偏好查询优化（最常用）
CREATE INDEX `idx_user_type_weight` ON `user_ai_preferences` (`user_id`, `preference_type`, `preference_weight` DESC);

-- 偏好置信度排序
CREATE INDEX `idx_user_confidence` ON `user_ai_preferences` (`user_id`, `confidence` DESC);

-- 偏好来源分析
CREATE INDEX `idx_source_confidence` ON `user_ai_preferences` (`learning_source`, `confidence` DESC);

-- 偏好更新时间查询
CREATE INDEX `idx_user_updated` ON `user_ai_preferences` (`user_id`, `updated_time`);

-- 7. 组合索引优化（针对复杂查询场景）
-- 菜品推荐核心查询：根据标签类型、权重、菜品价格筛选
CREATE INDEX `idx_tags_dish_complex` ON `dish_tags` (`tag_type`, `weight` DESC, `dish_id`);

-- 对话上下文查询：会话ID + 创建时间 + 状态
CREATE INDEX `idx_conversation_complex` ON `ai_conversation` (`session_id`, `created_time` DESC, `conversation_state`);

-- 推荐效果分析：算法 + 分数 + 反馈 + 时间
CREATE INDEX `idx_recommendation_analysis` ON `ai_recommendation_log` (`recommendation_algorithm`, `recommendation_score` DESC, `user_feedback`, `created_time`);

-- 8. 全文索引（用于文本搜索，如果支持）
-- 注意：MySQL 5.7+支持JSON字段的全文索引，但需要谨慎使用
-- 菜品描述全文搜索
ALTER TABLE `dish` ADD FULLTEXT INDEX `ft_description` (`description`);

-- AI回复内容全文搜索（用于分析AI回复质量）
ALTER TABLE `ai_conversation` ADD FULLTEXT INDEX `ft_ai_response` (`ai_response`);

-- 9. 性能监控相关索引
-- 慢查询分析用索引
CREATE INDEX `idx_performance_monitoring` ON `ai_conversation` (`response_time` DESC, `created_time`);

-- Token使用分析
CREATE INDEX `idx_token_analysis` ON `ai_conversation` (`created_time`, `response_time`);

-- 10. 分区建议（注释形式，需要根据数据量决定是否启用）
-- 大数据量情况下，可以考虑按时间分区
/*
-- ai_conversation表按月分区
ALTER TABLE ai_conversation 
PARTITION BY RANGE (YEAR(created_time) * 100 + MONTH(created_time)) (
    PARTITION p202401 VALUES LESS THAN (202402),
    PARTITION p202402 VALUES LESS THAN (202403),
    PARTITION p202403 VALUES LESS THAN (202404),
    PARTITION p202404 VALUES LESS THAN (202405),
    PARTITION p202405 VALUES LESS THAN (202406),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- ai_recommendation_log表按月分区
ALTER TABLE ai_recommendation_log 
PARTITION BY RANGE (YEAR(created_time) * 100 + MONTH(created_time)) (
    PARTITION p202401 VALUES LESS THAN (202402),
    PARTITION p202402 VALUES LESS THAN (202403),
    PARTITION p202403 VALUES LESS THAN (202404),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
*/

-- ===============================================
-- 索引创建完成提示
-- ===============================================
SELECT 'AI智能餐饮客服系统索引优化完成！' AS 'Status';

-- 显示创建的索引统计
SELECT 
    table_name AS '表名',
    COUNT(*) AS '索引数量'
FROM information_schema.statistics 
WHERE table_schema = DATABASE() 
  AND table_name IN ('ai_conversation', 'dish_tags', 'ai_recommendation_log', 'dish', 'ai_model_stats', 'user_ai_preferences')
GROUP BY table_name
ORDER BY COUNT(*) DESC; 