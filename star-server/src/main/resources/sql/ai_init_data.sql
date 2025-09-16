-- ===============================================
-- AI智能餐饮客服系统 - 初始化数据脚本
-- 版本: 1.0
-- 创建时间: 2024-01-01
-- 说明: 初始化菜品标签、测试数据等
-- ===============================================

-- 1. 更新现有菜品的描述信息（用于RAG知识库）
-- 注意：实际使用时需要根据具体菜品ID调整
UPDATE `dish` SET 
    `description` = '经典川菜代表，豆腐嫩滑，麻辣鲜香，口感层次丰富。选用优质嫩豆腐配上特制麻婆调料，麻而不木，辣而不燥，是下饭神器。',
    `nutrition_info` = JSON_OBJECT('calories', 180, 'protein', 12, 'fat', 8, 'carbs', 15, 'fiber', 3),
    `difficulty_level` = 2,
    `cooking_time` = 15,
    `spice_level` = 4,
    `ai_tags` = JSON_ARRAY('麻辣', '川菜', '下饭', '素食友好')
WHERE `name` = '麻婆豆腐';

UPDATE `dish` SET 
    `description` = '宫保鸡丁是川菜传统名菜，鸡肉鲜嫩，花生酥脆，酸甜微辣，口感丰富。精选鸡胸肉配花生米，调味独特，营养均衡。',
    `nutrition_info` = JSON_OBJECT('calories', 280, 'protein', 25, 'fat', 15, 'carbs', 12, 'fiber', 2),
    `difficulty_level` = 3,
    `cooking_time` = 20,
    `spice_level` = 3,
    `ai_tags` = JSON_ARRAY('微辣', '川菜', '鸡肉', '坚果')
WHERE `name` = '宫保鸡丁';

UPDATE `dish` SET 
    `description` = '水煮鱼是川菜重口味代表，鱼肉嫩滑，汤底麻辣鲜香，配菜丰富。精选草鱼片配豆芽、白菜等蔬菜，麻辣过瘾。',
    `nutrition_info` = JSON_OBJECT('calories', 320, 'protein', 28, 'fat', 20, 'carbs', 8, 'fiber', 4),
    `difficulty_level` = 4,
    `cooking_time` = 30,
    `spice_level` = 5,
    `ai_tags` = JSON_ARRAY('重辣', '川菜', '鱼类', '汤菜')
WHERE `name` = '水煮鱼';

-- 2. 插入菜品标签数据（基于常见菜品）
-- 川菜类标签
INSERT INTO `dish_tags` (`dish_id`, `tag_name`, `tag_type`, `weight`, `source`) VALUES
-- 假设麻婆豆腐的dish_id为1，实际使用时需要查询真实ID
(1, '麻辣', 'taste', 1.00, 'manual'),
(1, '川菜', 'cuisine', 1.00, 'manual'),
(1, '豆制品', 'feature', 0.80, 'manual'),
(1, '下饭', 'scene', 0.90, 'manual'),
(1, '素食友好', 'feature', 0.70, 'manual'),

-- 宫保鸡丁标签（假设dish_id为2）
(2, '微辣', 'taste', 0.80, 'manual'),
(2, '川菜', 'cuisine', 1.00, 'manual'),
(2, '鸡肉', 'feature', 1.00, 'manual'),
(2, '酸甜', 'taste', 0.60, 'manual'),
(2, '坚果', 'feature', 0.70, 'manual'),

-- 水煮鱼标签（假设dish_id为3）
(3, '重辣', 'taste', 1.00, 'manual'),
(3, '川菜', 'cuisine', 1.00, 'manual'),
(3, '鱼类', 'feature', 1.00, 'manual'),
(3, '汤菜', 'feature', 0.90, 'manual'),
(3, '聚餐', 'scene', 0.80, 'manual');

-- 3. 通用标签数据（口味类型）
-- 这些是常见的口味标签，可以根据实际菜品分配，select的结果是要插入的数据
INSERT INTO `dish_tags` (`dish_id`, `tag_name`, `tag_type`, `weight`, `source`) 
SELECT d.id, '清淡', 'taste', 0.90, 'ai'
FROM `dish` d 
WHERE d.name IN ('白粥', '蒸蛋', '清汤面条') 
LIMIT 10;

-- 甜味标签
INSERT INTO `dish_tags` (`dish_id`, `tag_name`, `tag_type`, `weight`, `source`)
SELECT d.id, '甜味', 'taste', 0.85, 'ai'
FROM `dish` d 
WHERE d.name LIKE '%糖%' OR d.name LIKE '%甜%' 
LIMIT 10;

-- 4. 菜系分类标签
INSERT INTO `dish_tags` (`dish_id`, `tag_name`, `tag_type`, `weight`, `source`)
SELECT d.id, '粤菜', 'cuisine', 1.00, 'ai'
FROM `dish` d 
WHERE d.name LIKE '%白切%' OR d.name LIKE '%煲%' OR d.name LIKE '%烧%'
LIMIT 10;

INSERT INTO `dish_tags` (`dish_id`, `tag_name`, `tag_type`, `weight`, `source`)
SELECT d.id, '湘菜', 'cuisine', 1.00, 'ai'
FROM `dish` d 
WHERE d.name LIKE '%辣椒%' OR d.name LIKE '%湘%'
LIMIT 5;

-- 5. 场景标签
INSERT INTO `dish_tags` (`dish_id`, `tag_name`, `tag_type`, `weight`, `source`)
SELECT d.id, '商务宴请', 'scene', 0.80, 'ai'
FROM `dish` d 
WHERE d.price >= 50
LIMIT 10;

INSERT INTO `dish_tags` (`dish_id`, `tag_name`, `tag_type`, `weight`, `source`)
SELECT d.id, '家庭聚餐', 'scene', 0.90, 'ai'
FROM `dish` d 
WHERE d.price BETWEEN 20 AND 45
LIMIT 15;

INSERT INTO `dish_tags` (`dish_id`, `tag_name`, `tag_type`, `weight`, `source`)
SELECT d.id, '快餐', 'scene', 0.95, 'ai'
FROM `dish` d 
WHERE d.price <= 25
LIMIT 10;

-- 6. 营养特色标签
INSERT INTO `dish_tags` (`dish_id`, `tag_name`, `tag_type`, `weight`, `source`)
SELECT d.id, '高蛋白', 'nutrition', 0.85, 'ai'
FROM `dish` d 
WHERE d.name LIKE '%鸡%' OR d.name LIKE '%鱼%' OR d.name LIKE '%肉%'
LIMIT 20;

INSERT INTO `dish_tags` (`dish_id`, `tag_name`, `tag_type`, `weight`, `source`)
SELECT d.id, '素食', 'nutrition', 1.00, 'ai'
FROM `dish` d 
WHERE d.name LIKE '%豆腐%' OR d.name LIKE '%青菜%' OR d.name LIKE '%萝卜%'
LIMIT 15;

-- 7. 插入一些测试对话数据
INSERT INTO `ai_conversation` (`session_id`, `user_id`, `user_message`, `ai_response`, `conversation_context`, `conversation_state`, `model_info`, `response_time`, `token_usage`) VALUES
('test_session_001', 1, '我想要2个人吃辣一点的川菜', '好的！为2位客人推荐几道经典川菜：麻婆豆腐、宫保鸡丁、水煮鱼片。这些菜品口感麻辣，很适合喜欢辣味的朋友。', 
 JSON_OBJECT('userRequirement', JSON_OBJECT('peopleCount', 2, 'tastePreferences', JSON_ARRAY('辣'), 'cuisineType', '川菜')), 
 'RECOMMENDATION',
 JSON_OBJECT('model', 'qwen-turbo', 'version', '1.0', 'provider', 'dashscope'),
 2800,
 JSON_OBJECT('input_tokens', 45, 'output_tokens', 78, 'total_tokens', 123)),

('test_session_002', NULL, '有什么清淡的菜推荐吗？', '为您推荐几道清淡的菜品：白粥配咸菜、蒸蛋羹、清炒时蔬。这些菜品口味清淡，营养丰富，适合养胃。', 
 JSON_OBJECT('userRequirement', JSON_OBJECT('tastePreferences', JSON_ARRAY('清淡'))), 
 'RECOMMENDATION',
 JSON_OBJECT('model', 'deepseek-chat', 'version', '1.0', 'provider', 'deepseek'),
 1900,
 JSON_OBJECT('input_tokens', 32, 'output_tokens', 56, 'total_tokens', 88));

-- 8. 插入测试推荐记录
INSERT INTO `ai_recommendation_log` (`session_id`, `user_id`, `user_requirements`, `recommended_dishes`, `recommendation_algorithm`, `recommendation_score`, `rag_enabled`, `user_feedback`, `total_price`) VALUES
('test_session_001', 1, 
 JSON_OBJECT('peopleCount', 2, 'tastePreferences', JSON_ARRAY('辣'), 'cuisineType', '川菜'),
 JSON_ARRAY(
     JSON_OBJECT('dishId', 1, 'dishName', '麻婆豆腐', 'price', 28.00, 'score', 0.95, 'reason', '符合您的辣味偏好'),
     JSON_OBJECT('dishId', 2, 'dishName', '宫保鸡丁', 'price', 32.00, 'score', 0.88, 'reason', '经典川菜，口感丰富'),
     JSON_OBJECT('dishId', 3, 'dishName', '水煮鱼', 'price', 48.00, 'score', 0.92, 'reason', '重口味川菜代表')
 ),
 'content_based_rag', 0.92, TRUE, 1, 108.00);

-- 9. 插入AI模型统计数据
INSERT INTO `ai_model_stats` (`model_name`, `call_type`, `call_date`, `call_count`, `total_tokens`, `total_response_time`, `success_count`, `error_count`, `avg_response_time`) VALUES
('qwen-turbo', 'understanding', CURDATE(), 50, 2500, 125000, 48, 2, 2500.00),
('deepseek-chat', 'recommendation', CURDATE(), 35, 4200, 98000, 34, 1, 2800.00),
('qwen-turbo', 'response', CURDATE(), 45, 3100, 90000, 45, 0, 2000.00);

-- 10. 插入测试用户偏好数据
INSERT INTO `user_ai_preferences` (`user_id`, `preference_type`, `preference_value`, `preference_weight`, `learning_source`, `confidence`) VALUES
(1, 'taste', '辣', 0.90, 'ai', 0.85),
(1, 'cuisine', '川菜', 0.80, 'order', 0.92),
(1, 'price', '中等', 0.70, 'ai', 0.75),
(2, 'taste', '清淡', 0.85, 'manual', 0.95),
(2, 'nutrition', '素食', 0.90, 'manual', 1.00);

-- ===============================================
-- 数据初始化完成提示
-- ===============================================
SELECT 'AI智能餐饮客服系统数据初始化完成！' AS 'Status';
SELECT COUNT(*) as 'Total Dish Tags' FROM dish_tags;
SELECT COUNT(*) as 'Test Conversations' FROM ai_conversation;
SELECT COUNT(*) as 'Recommendation Logs' FROM ai_recommendation_log; 