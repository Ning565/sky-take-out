-- ===============================================
-- AI智能餐饮客服系统 - 数据表创建脚本
-- 版本: 1.0
-- 创建时间: 2024-01-01
-- 说明: 用于AI对话、推荐、标签等功能的数据表
-- ===============================================

-- 1. AI对话记录表
DROP TABLE IF EXISTS `ai_conversation`;
CREATE TABLE `ai_conversation` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '对话记录ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID，用于关联同一次对话',
    `user_id` BIGINT NULL COMMENT '用户ID（可选，用于登录用户）',
    `user_message` TEXT NOT NULL COMMENT '用户输入的消息',
    `ai_response` TEXT NOT NULL COMMENT 'AI回复的消息',
    `conversation_context` JSON COMMENT '对话上下文数据（包含Agent调用信息、模型信息等）',
    `conversation_state` VARCHAR(50) DEFAULT 'GREETING' COMMENT '对话状态（GREETING/REQUIREMENT_GATHERING/RECOMMENDATION/CLARIFICATION/ORDER_GUIDANCE/COMPLETED）',
    `model_info` JSON COMMENT '使用的模型信息（模型名称、版本、调用时间等）',
    `response_time` INT DEFAULT 0 COMMENT '响应时间（毫秒）',
    `token_usage` JSON COMMENT 'Token使用情况（输入tokens、输出tokens、总tokens）',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_time` (`created_time`),
    INDEX `idx_conversation_state` (`conversation_state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话记录表';

-- 2. 菜品标签表（用于RAG知识库和推荐算法）
DROP TABLE IF EXISTS `dish_tags`;
CREATE TABLE `dish_tags` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    `dish_id` BIGINT NOT NULL COMMENT '菜品ID，关联dish表',
    `tag_name` VARCHAR(50) NOT NULL COMMENT '标签名称（如：辣、甜、川菜、素食等）',
    `tag_type` VARCHAR(20) NOT NULL COMMENT '标签类型（taste:口味、cuisine:菜系、feature:特色、nutrition:营养、scene:场景）',
    `tag_value` VARCHAR(100) NULL COMMENT '标签值（可选，用于数值型标签如辣度等级）',
    `weight` DECIMAL(3,2) DEFAULT 1.00 COMMENT '标签权重（0.01-1.00，用于推荐算法）',
    `confidence` DECIMAL(3,2) DEFAULT 1.00 COMMENT '标签置信度（0.01-1.00，AI自动标注的置信度）',
    `source` VARCHAR(20) DEFAULT 'manual' COMMENT '标签来源（manual:人工标注、ai:AI自动标注、import:批量导入）',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX `idx_dish_id` (`dish_id`),
    INDEX `idx_tag_name` (`tag_name`),
    INDEX `idx_tag_type` (`tag_type`),
    INDEX `idx_dish_tag_type` (`dish_id`, `tag_type`),
    FOREIGN KEY (`dish_id`) REFERENCES `dish`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品标签表';

-- 3. AI推荐记录表（用于推荐效果分析和优化）
DROP TABLE IF EXISTS `ai_recommendation_log`;
CREATE TABLE `ai_recommendation_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '推荐记录ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
    `user_id` BIGINT NULL COMMENT '用户ID',
    `user_requirements` JSON NOT NULL COMMENT '用户需求（结构化数据：人数、口味、菜系、预算等）',
    `recommended_dishes` JSON NOT NULL COMMENT '推荐的菜品列表（dish_id、推荐分数、推荐理由等）',
    `recommendation_algorithm` VARCHAR(50) DEFAULT 'content_based' COMMENT '推荐算法类型',
    `recommendation_score` DECIMAL(3,2) COMMENT '整体推荐置信度（0.01-1.00）',
    `rag_enabled` BOOLEAN DEFAULT TRUE COMMENT '是否使用了RAG检索',
    `rag_context` JSON COMMENT 'RAG检索的上下文信息',
    `agent_chain` JSON COMMENT 'Agent调用链信息',
    `user_feedback` TINYINT NULL COMMENT '用户反馈（1:满意、0:不满意、-1:未反馈）',
    `feedback_comment` TEXT NULL COMMENT '用户反馈评论',
    `clicked_dishes` JSON NULL COMMENT '用户点击的菜品列表',
    `ordered_dishes` JSON NULL COMMENT '用户最终下单的菜品列表',
    `total_price` DECIMAL(10,2) NULL COMMENT '推荐菜品总价格',
    `actual_order_price` DECIMAL(10,2) NULL COMMENT '实际下单金额',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_time` (`created_time`),
    INDEX `idx_user_feedback` (`user_feedback`),
    INDEX `idx_recommendation_algorithm` (`recommendation_algorithm`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI推荐记录表';

-- 4. 扩展现有dish表，添加AI相关字段
-- 注意：这里使用ALTER语句，如果字段已存在会报错，实际使用时需要检查
ALTER TABLE `dish` 
ADD COLUMN `description` TEXT COMMENT '菜品详细描述（用于RAG知识库）' AFTER `image`,
ADD COLUMN `nutrition_info` JSON COMMENT '营养信息（热量、蛋白质、脂肪、碳水化合物等）' AFTER `description`,
ADD COLUMN `difficulty_level` TINYINT DEFAULT 1 COMMENT '制作难度等级（1-5级，1最简单）' AFTER `nutrition_info`,
ADD COLUMN `cooking_time` INT DEFAULT 0 COMMENT '制作时间（分钟）' AFTER `difficulty_level`,
ADD COLUMN `spice_level` TINYINT DEFAULT 0 COMMENT '辣度等级（0-5，0不辣）' AFTER `cooking_time`,
ADD COLUMN `recommend_count` INT DEFAULT 0 COMMENT '被AI推荐次数' AFTER `spice_level`,
ADD COLUMN `order_count_via_ai` INT DEFAULT 0 COMMENT '通过AI推荐下单次数' AFTER `recommend_count`,
ADD COLUMN `ai_tags` JSON COMMENT 'AI自动提取的标签（缓存常用标签）' AFTER `order_count_via_ai`,
ADD COLUMN `vector_updated_time` DATETIME NULL COMMENT '向量化更新时间' AFTER `ai_tags`;

-- 添加相关索引
ALTER TABLE `dish` 
ADD INDEX `idx_spice_level` (`spice_level`),
ADD INDEX `idx_difficulty_level` (`difficulty_level`),
ADD INDEX `idx_recommend_count` (`recommend_count`),
ADD INDEX `idx_vector_updated_time` (`vector_updated_time`);

-- 5. AI模型调用统计表（用于监控和优化）
DROP TABLE IF EXISTS `ai_model_stats`;
CREATE TABLE `ai_model_stats` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '统计记录ID',
    `model_name` VARCHAR(50) NOT NULL COMMENT '模型名称（qwen-turbo、deepseek-chat等）',
    `call_type` VARCHAR(30) NOT NULL COMMENT '调用类型（chat、embedding、understanding、recommendation、response）',
    `call_date` DATE NOT NULL COMMENT '调用日期',
    `call_count` INT DEFAULT 0 COMMENT '调用次数',
    `total_tokens` BIGINT DEFAULT 0 COMMENT '总Token消耗',
    `total_response_time` BIGINT DEFAULT 0 COMMENT '总响应时间（毫秒）',
    `success_count` INT DEFAULT 0 COMMENT '成功调用次数',
    `error_count` INT DEFAULT 0 COMMENT '失败调用次数',
    `avg_response_time` DECIMAL(8,2) DEFAULT 0 COMMENT '平均响应时间（毫秒）',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY `uk_model_type_date` (`model_name`, `call_type`, `call_date`),
    INDEX `idx_call_date` (`call_date`),
    INDEX `idx_model_name` (`model_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI模型调用统计表';

-- 6. 用户AI偏好表（用于个性化推荐）
DROP TABLE IF EXISTS `user_ai_preferences`;
CREATE TABLE `user_ai_preferences` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '偏好记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `preference_type` VARCHAR(20) NOT NULL COMMENT '偏好类型（taste:口味、cuisine:菜系、price:价格、nutrition:营养等）',
    `preference_value` VARCHAR(100) NOT NULL COMMENT '偏好值',
    `preference_weight` DECIMAL(3,2) DEFAULT 1.00 COMMENT '偏好权重（0.01-1.00）',
    `learning_source` VARCHAR(20) DEFAULT 'ai' COMMENT '学习来源（ai:AI学习、manual:用户设置、order:订单行为）',
    `confidence` DECIMAL(3,2) DEFAULT 0.50 COMMENT '置信度（0.01-1.00）',
    `last_confirmed_time` DATETIME NULL COMMENT '最后确认时间',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY `uk_user_type_value` (`user_id`, `preference_type`, `preference_value`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_preference_type` (`preference_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户AI偏好表';

-- ===============================================
-- 创建完成提示
-- ===============================================
SELECT 'AI智能餐饮客服系统数据表创建完成！' AS 'Status';
SELECT 'Please run ai_init_data.sql to initialize data.' AS 'Next Step';
