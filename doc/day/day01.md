# 新增AI工具的作用

我将**结合本项目“智能餐饮客服”业务场景**，具体分析Agent、RAG、MCP三者在本项目中的实际作用、必要性，以及在项目规划中的定位和取舍建议。

---

## 1. Agent（多智能体协作系统）

### 在本项目中的作用
- **业务定位**：Agent系统是“智能客服”业务流程的主线驱动者。它将用户的自然语言需求，分阶段拆解为“理解→推荐→回复”三大环节，每个环节由专门的Agent负责，流水线式协作。
- **业务价值**：
  - **用户输入多样**，需要NLU（理解Agent）将其结构化，才能驱动后续推荐。
  - **推荐Agent**负责调用RAG和推荐算法，输出个性化菜品组合。
  - **回复Agent**将推荐结果转化为自然语言，提升对话体验。
- **举例**：
  - 用户说“我们三个人想吃辣的川菜，预算人均50”，理解Agent提取结构化需求，推荐Agent检索并筛选菜品，回复Agent生成“为您推荐麻婆豆腐、宫保鸡丁...”的回复。

### 是否必须？
- **必须**。如果没有Agent分工，所有逻辑会混杂在一起，难以维护和扩展，且无法体现“多智能体协作”的AI工程能力（这也是简历亮点）。

---

## 2. RAG（检索增强生成）

### 在本项目中的作用
- **业务定位**：RAG是“智能推荐”的知识支撑。它让AI推荐和回复不再凭空“编造”，而是基于真实的菜品库、标签库等知识，提升推荐的专业性和可信度。
- **业务价值**：
  - **菜品信息、标签、场景等知识**存储在数据库/向量库，RAG负责检索最相关的内容，作为大模型生成的上下文。
  - **个性化推荐**：不同用户、不同需求，检索到的知识不同，推荐更精准。
  - **防止AI胡说八道**：比如不会推荐菜单上没有的菜。
- **举例**：
  - 用户问“适合小孩吃的粤菜”，RAG检索“粤菜+儿童友好”标签的菜品，推荐Agent基于这些知识生成推荐。

### 是否必须？
- **强烈建议保留**。RAG是现代AI问答/推荐系统的核心，能极大提升业务专业性和用户信任度。如果去掉RAG，推荐和回复会变得“虚”，不适合餐饮业务场景。

---

## 3. MCP（模型控制协议）

### 在本项目中的作用
- **业务定位**：MCP是“多大模型能力调度与治理”的基础设施。它让项目可以灵活切换/并发调用通义千问、DeepSeek、OpenAI等不同大模型，保障高可用、低成本和可扩展性。
- **业务价值**：
  - **模型切换**：如通义千问不稳定时自动切换到DeepSeek。
  - **负载均衡/降本**：高峰期用便宜模型，关键场景用高质量模型。
  - **多模型对比/融合**：可用于A/B测试、效果评估。
- **举例**：
  - 某天通义千问API超时，MCP自动切换到DeepSeek，用户无感知。
  - 业务需要对比不同模型的推荐效果，MCP支持多模型并发调用。

### 是否必须？
- **可选**，但**强烈建议保留**，尤其是：
  - 你希望简历突出“多大模型调度、工程治理能力”；
  - 你希望项目具备“高可用、可扩展、低成本”特性；
  - 你未来可能接入更多大模型或做A/B实验。
- **如果只是单一模型、单一场景的小项目，可以简化MCP，但会损失工程深度和亮点。**

---

## 项目规划中的定位与建议

| 模块  | 业务主线            | 必要性           | 价值                         | 是否建议保留 |
| ----- | ------------------- | ---------------- | ---------------------------- | ------------ |
| Agent | 业务流程分工与协作  | 必须             | 结构清晰、易扩展、AI工程亮点 | 必须         |
| RAG   | 推荐/回复的知识支撑 | 必须             | 推荐专业、可信、个性化       | 必须         |
| MCP   | 多大模型调度与治理  | 可选（强烈建议） | 高可用、低成本、工程深度     | 建议保留     |

### 总结
- **Agent**和**RAG**是智能餐饮客服业务的核心，缺一不可。
- **MCP**是工程治理和多模型能力的体现，虽然不是业务主线，但对简历和项目深度极有帮助，建议保留。
- 如果只做最简化的“单模型+单流程”Demo，MCP可以弱化，但会损失技术亮点。

---

**结论：**
- 三者在本项目中各司其职，互为补充，建议全部保留，且在项目描述和简历中突出它们的业务价值和工程能力。
- 如果必须删减，优先保留Agent和RAG，MCP可根据实际资源和目标适当简化。

如需进一步细化每个模块的业务流、代码结构或删减建议，请继续说明！day 01 环境配置

## AI应用环境配置

### ai相关配置文件

包括ai服务开发用到的数据库：向量数据库用于存储文本转换成的向量embedding，AI系统需要快速找到相似的内容，embedding后可以通过数学计算找到相似度高的内容，使用redis的一号数据库作为存储

大模型model信息，rag的相似度阈值：

- **similarity-threshold: 0.6**

  - **含义**：当用户提问时，系统会在知识库中查找相似内容

  - **0.6的意思**：相似度得分范围是0-1，0.6表示60%相似就认为相关
- **batch-size: 5**

  - **含义**：一次处理5条文本转向量
  - **举例**：有100条文本需要向量化，会分成20批，每批5条

- Agent设置的回复响应超时时间：15秒超时

- 用户对话上下文保持时间和记录详情日志

```yaml

  # AI服务开发环境配置
  ai-dev:
    # 向量存储使用Redis 1号数据库
    vector-database: 1
  wechat1:
    appid: wx58502940b2c06581
    secret: a239d112e0270f41562c6004a262d2d1
  mybatis-plus:
    mapper-locations: classpath:/mapper/*.xml # 如果使用 XML 映射文件
    configuration:
      log-impl: org.apache.ibatis.logging.stdout.StdOutImpl # 开启 SQL 日志
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    listener:
      simple:
        acknowledge-mode: manual

# 开发环境大模型API密钥（从环境变量获取，不提交到代码库）
# 需要在开发环境设置以下环境变量：
# export DASHSCOPE_API_KEY="your_dashscope_api_key"  
# export DEEPSEEK_API_KEY="your_deepseek_api_key"
# export OPENAI_API_KEY="your_openai_api_key"

# 开发环境AI服务配置覆盖
llm:
  dashscope:
    chat:
      model: qwen-turbo  # 开发环境使用较快的模型
  deepseek:
    chat:
      model: deepseek-chat
  rag:
    vector-store:
      similarity-threshold: 0.6  # 开发环境降低阈值，便于测试
    embedding:
      batch-size: 5  # 开发环境减少批处理大小
  agents:
    understanding:
      timeout: 15s  # 开发环境增加超时时间，便于调试
    recommendation:
      timeout: 20s
    response:
      timeout: 15s

ai:
  conversation:
    session-timeout: 3600  # 开发环境延长会话时间，便于测试
  monitoring:
    log-model-calls: true  # 开发环境启用详细日志
```

## 实体类与数据库搭建

### 数据库1：建立新表

#### 1. AI对话记录表 (ai_conversation)

| 字段名               | 类型        | 必填 | 默认值            | 说明           |
| :------------------- | :---------- | :--- | :---------------- | :------------- |
| id                   | BIGINT      | 是   | 自增              | 对话记录ID     |
| session_id           | VARCHAR(64) | 是   | -                 | 会话ID         |
| user_id              | BIGINT      | 否   | -                 | 用户ID         |
| user_message         | TEXT        | 是   | -                 | 用户输入消息   |
| ai_response          | TEXT        | 是   | -                 | AI回复消息     |
| conversation_context | JSON        | 否   | -                 | 对话上下文数据 |
| conversation_state   | VARCHAR(50) | 否   | 'GREETING'        | 对话状态       |
| model_info           | JSON        | 否   | -                 | 模型信息       |
| response_time        | INT         | 否   | 0                 | 响应时间(ms)   |
| token_usage          | JSON        | 否   | -                 | Token使用情况  |
| created_time         | DATETIME    | 否   | CURRENT_TIMESTAMP | 创建时间       |
| updated_time         | DATETIME    | 否   | CURRENT_TIMESTAMP | 更新时间       |

#### 2. 菜品标签表 (dish_tags)

| 字段名       | 类型         | 必填 | 默认值            | 说明     |
| :----------- | :----------- | :--- | :---------------- | :------- |
| id           | BIGINT       | 是   | 自增              | 标签ID   |
| dish_id      | BIGINT       | 是   | -                 | 菜品ID   |
| tag_name     | VARCHAR(50)  | 是   | -                 | 标签名称 |
| tag_type     | VARCHAR(20)  | 是   | -                 | 标签类型 |
| tag_value    | VARCHAR(100) | 否   | -                 | 标签值   |
| weight       | DECIMAL(3,2) | 否   | 1.00              | 标签权重 |
| confidence   | DECIMAL(3,2) | 否   | 1.00              | 置信度   |
| source       | VARCHAR(20)  | 否   | 'manual'          | 标签来源 |
| created_time | DATETIME     | 否   | CURRENT_TIMESTAMP | 创建时间 |
| updated_time | DATETIME     | 否   | CURRENT_TIMESTAMP | 更新时间 |

#### 3. AI推荐记录表 (ai_recommendation_log)

| 字段名                   | 类型          | 必填 | 默认值            | 说明         |
| :----------------------- | :------------ | :--- | :---------------- | :----------- |
| id                       | BIGINT        | 是   | 自增              | 推荐记录ID   |
| session_id               | VARCHAR(64)   | 是   | -                 | 会话ID       |
| user_id                  | BIGINT        | 否   | -                 | 用户ID       |
| user_requirements        | JSON          | 是   | -                 | 用户需求     |
| recommended_dishes       | JSON          | 是   | -                 | 推荐菜品列表 |
| recommendation_algorithm | VARCHAR(50)   | 否   | 'content_based'   | 推荐算法     |
| recommendation_score     | DECIMAL(3,2)  | 否   | -                 | 推荐置信度   |
| rag_enabled              | BOOLEAN       | 否   | TRUE              | 是否使用RAG  |
| rag_context              | JSON          | 否   | -                 | RAG上下文    |
| agent_chain              | JSON          | 否   | -                 | Agent调用链  |
| user_feedback            | TINYINT       | 否   | -                 | 用户反馈     |
| feedback_comment         | TEXT          | 否   | -                 | 反馈评论     |
| clicked_dishes           | JSON          | 否   | -                 | 点击菜品列表 |
| ordered_dishes           | JSON          | 否   | -                 | 下单菜品列表 |
| total_price              | DECIMAL(10,2) | 否   | -                 | 推荐总价     |
| actual_order_price       | DECIMAL(10,2) | 否   | -                 | 实际金额     |
| created_time             | DATETIME      | 否   | CURRENT_TIMESTAMP | 创建时间     |
| updated_time             | DATETIME      | 否   | CURRENT_TIMESTAMP | 更新时间     |

#### 4. 菜品表扩展字段 (dish)

| 新增字段            | 类型     | 默认值 | 说明           |
| :------------------ | :------- | :----- | :------------- |
| description         | TEXT     | -      | 菜品详细描述   |
| nutrition_info      | JSON     | -      | 营养信息       |
| difficulty_level    | TINYINT  | 1      | 制作难度(1-5)  |
| cooking_time        | INT      | 0      | 制作时间(分钟) |
| spice_level         | TINYINT  | 0      | 辣度等级(0-5)  |
| recommend_count     | INT      | 0      | 被AI推荐次数   |
| order_count_via_ai  | INT      | 0      | AI推荐下单次数 |
| ai_tags             | JSON     | -      | AI自动提取标签 |
| vector_updated_time | DATETIME | -      | 向量化更新时间 |

#### 5. AI模型调用统计表 (ai_model_stats)

| 字段名              | 类型         | 必填 | 默认值            | 说明         |
| :------------------ | :----------- | :--- | :---------------- | :----------- |
| id                  | BIGINT       | 是   | 自增              | 统计记录ID   |
| model_name          | VARCHAR(50)  | 是   | -                 | 模型名称     |
| call_type           | VARCHAR(30)  | 是   | -                 | 调用类型     |
| call_date           | DATE         | 是   | -                 | 调用日期     |
| call_count          | INT          | 否   | 0                 | 调用次数     |
| total_tokens        | BIGINT       | 否   | 0                 | 总Token消耗  |
| total_response_time | BIGINT       | 否   | 0                 | 总响应时间   |
| success_count       | INT          | 否   | 0                 | 成功次数     |
| error_count         | INT          | 否   | 0                 | 失败次数     |
| avg_response_time   | DECIMAL(8,2) | 否   | 0                 | 平均响应时间 |
| created_time        | DATETIME     | 否   | CURRENT_TIMESTAMP | 创建时间     |
| updated_time        | DATETIME     | 否   | CURRENT_TIMESTAMP | 更新时间     |

#### 6. 用户AI偏好表 (user_ai_preferences)

| 字段名              | 类型         | 必填 | 默认值            | 说明         |
| :------------------ | :----------- | :--- | :---------------- | :----------- |
| id                  | BIGINT       | 是   | 自增              | 偏好记录ID   |
| user_id             | BIGINT       | 是   | -                 | 用户ID       |
| preference_type     | VARCHAR(20)  | 是   | -                 | 偏好类型     |
| preference_value    | VARCHAR(100) | 是   | -                 | 偏好值       |
| preference_weight   | DECIMAL(3,2) | 否   | 1.00              | 偏好权重     |
| learning_source     | VARCHAR(20)  | 否   | 'ai'              | 学习来源     |
| confidence          | DECIMAL(3,2) | 否   | 0.50              | 置信度       |
| last_confirmed_time | DATETIME     | 否   | -                 | 最后确认时间 |
| created_time        | DATETIME     | 否   | CURRENT_TIMESTAMP | 创建时间     |
| updated_time        | DATETIME     | 否   | CURRENT_TIMESTAMP | 更新时间     |

### 数据库2：更新数据

**为dish（菜品）表新增字段， 更新现有菜品的描述信息（用于RAG知识库）**
如：

```sql
UPDATE `dish` SET 
    `description` = '经典川菜代表，豆腐嫩滑，麻辣鲜香，口感层次丰富。选用优质嫩豆腐配上特制麻婆调料，麻而不木，辣而不燥，是下饭神器。',
    `nutrition_info` = JSON_OBJECT('calories', 180, 'protein', 12, 'fat', 8, 'carbs', 15, 'fiber', 3),
    `difficulty_level` = 2,
    `cooking_time` = 15,
    `spice_level` = 4,
    `ai_tags` = JSON_ARRAY('麻辣', '川菜', '下饭', '素食友好')
WHERE `name` = '麻婆豆腐';
```

**插入菜品标签数据（基于常见菜品）：为菜品tags表插入相对应的信息**

如：

``` sql
-- 川菜类标签
INSERT INTO `dish_tags` (`dish_id`, `tag_name`, `tag_type`, `weight`, `source`) VALUES
-- 假设麻婆豆腐的dish_id为1，实际使用时需要查询真实ID
(1, '麻辣', 'taste', 1.00, 'manual'),
(1, '川菜', 'cuisine', 1.00, 'manual'),
(1, '豆制品', 'feature', 0.80, 'manual'),
(1, '下饭', 'scene', 0.90, 'manual'),
(1, '素食友好', 'feature', 0.70, 'manual'),
```

**为dish_tags插入菜品查询到的数据，建立通用标签数据**

如：

``` sql
-- 这些是常见的口味标签，可以根据实际菜品分配，select的结果是要插入的数据
INSERT INTO `dish_tags` (`dish_id`, `tag_name`, `tag_type`, `weight`, `source`) 
SELECT d.id, '清淡', 'taste', 0.90, 'ai'
FROM `dish` d 
WHERE d.name IN ('白粥', '蒸蛋', '清汤面条') 
LIMIT 10;
```

**针对tag_type：(若干个tag_name，如taste:{甜味，清淡...})增加数据**

**插入一些测试对话数据**

``` sql
INSERT INTO `ai_conversation` (`session_id`, `user_id`, `user_message`, `ai_response`, `conversation_context`, `conversation_state`, `model_info`, `response_time`, `token_usage`) VALUES

('test_session_001', 1, '我想要2个人吃辣一点的川菜', '好的！为2位客人推荐几道经典川菜：麻婆豆腐、宫保鸡丁、水煮鱼片。这些菜品口感麻辣，很适合喜欢辣味的朋友。', 

 JSON_OBJECT('userRequirement', JSON_OBJECT('peopleCount', 2, 'tastePreferences', JSON_ARRAY('辣'), 'cuisineType', '川菜')), 

 'RECOMMENDATION',

 JSON_OBJECT('model', 'qwen-turbo', 'version', '1.0', 'provider', 'dashscope'),

 2800,

 JSON_OBJECT('input_tokens', 45, 'output_tokens', 78, 'total_tokens', 123)),
```

**插入测试推荐记录**

``` sql
INSERT INTO `ai_recommendation_log` (`session_id`, `user_id`, `user_requirements`, `recommended_dishes`, `recommendation_algorithm`, `recommendation_score`, `rag_enabled`, `user_feedback`, `total_price`) VALUES

('test_session_001', 1, 

 JSON_OBJECT('peopleCount', 2, 'tastePreferences', JSON_ARRAY('辣'), 'cuisineType', '川菜'),

 JSON_ARRAY(

​     JSON_OBJECT('dishId', 1, 'dishName', '麻婆豆腐', 'price', 28.00, 'score', 0.95, 'reason', '符合您的辣味偏好'),

​     JSON_OBJECT('dishId', 2, 'dishName', '宫保鸡丁', 'price', 32.00, 'score', 0.88, 'reason', '经典川菜，口感丰富'),

​     JSON_OBJECT('dishId', 3, 'dishName', '水煮鱼', 'price', 48.00, 'score', 0.92, 'reason', '重口味川菜代表')

 ),

 'content_based_rag', 0.92, TRUE, 1, 108.00);
```

**插入AI模型统计数据**

``` sql
INSERT INTO `ai_model_stats` (`model_name`, `call_type`, `call_date`, `call_count`, `total_tokens`, `total_response_time`, `success_count`, `error_count`, `avg_response_time`) VALUES

('qwen-turbo', 'understanding', CURDATE(), 50, 2500, 125000, 48, 2, 2500.00),

('deepseek-chat', 'recommendation', CURDATE(), 35, 4200, 98000, 34, 1, 2800.00),

('qwen-turbo', 'response', CURDATE(), 45, 3100, 90000, 45, 0, 2000.00);
```

**插入测试用户偏好数据**

``` sql
INSERT INTO `user_ai_preferences` (`user_id`, `preference_type`, `preference_value`, `preference_weight`, `learning_source`, `confidence`) VALUES

(1, 'taste', '辣', 0.90, 'ai', 0.85),

(1, 'cuisine', '川菜', 0.80, 'order', 0.92),

(1, 'price', '中等', 0.70, 'ai', 0.75),

(2, 'taste', '清淡', 0.85, 'manual', 0.95),

(2, 'nutrition', '素食', 0.90, 'manual', 1.00);
```

### 数据库3：增加索引

``` sql
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
```

### 实体类

Ai对话类的创建：
``` java
package com.star.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AiConversation {
    private Long id; // 对话记录ID
    private String sessionId; // 会话ID
    private Long userId; // 用户ID
    private String userMessage; // 用户输入的消息
    private String aiResponse; // AI回复的消息
    private String conversationContext; // 对话上下文数据（JSON）
    private String conversationState; // 对话状态
    private String modelInfo; // 使用的模型信息（JSON）
    private Integer responseTime; // 响应时间（毫秒）
    private String tokenUsage; // Token使用情况（JSON）
    private LocalDateTime createdTime; // 创建时间
    private LocalDateTime updatedTime; // 更新时间
} 

```

DishTag类：

``` java
package com.star.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DishTag {
    private Long id; // 标签ID
    private Long dishId; // 菜品ID
    private String tagName; // 标签名称
    private String tagType; // 标签类型
    private String tagValue; // 标签值
    private Double weight; // 标签权重
    private Double confidence; // 标签置信度
    private String source; // 标签来源
    private LocalDateTime createdTime; // 创建时间
    private LocalDateTime updatedTime; // 更新时间
} 
```

**菜品推荐类(单个)：**

``` java
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
```

**推荐结果类**：

``` java
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
```

**AI推荐日志类：**

``` java
package com.star.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AiRecommendationLog {
    private Long id; // 推荐记录ID
    private String sessionId; // 会话ID
    private Long userId; // 用户ID
    private String userRequirements; // 用户需求（JSON）
    private String recommendedDishes; // 推荐菜品列表（JSON）
    private String recommendationAlgorithm; // 推荐算法类型
    private Double recommendationScore; // 推荐置信度
    private Boolean ragEnabled; // 是否使用RAG
    private String ragContext; // RAG上下文信息
    private String agentChain; // Agent调用链信息
    private Integer userFeedback; // 用户反馈
    private String feedbackComment; // 用户反馈评论
    private String clickedDishes; // 用户点击的菜品列表（JSON）
    private String orderedDishes; // 用户下单菜品列表（JSON）
    private BigDecimal totalPrice; // 推荐菜品总价格
    private BigDecimal actualOrderPrice; // 实际下单金额
    private LocalDateTime createdTime; // 创建时间
    private LocalDateTime updatedTime; // 更新时间
} 
```

**对话请求：**

``` java
package com.star.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String sessionId; // 会话ID
    private String message;   // 用户输入消息
    private Long userId;      // 用户ID（可选）
} 
```

**对话回复**

``` java
package com.star.vo;

import lombok.Data;
import java.util.List;

@Data
public class ChatResponse {
    private String messageText; // AI回复文本
    private List<DishRecommendation> recommendations; // 推荐菜品列表
    private List<String> followUpQuestions; // 后续追问
    private String sessionId; // 会话ID
    private String conversationState; // 对话状态
} 
```



**用户需要类：**

``` java
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
```

## Mybatis Mapper接口和xml代码

dish_tag：

``` java
package com.star.mapper;

import com.star.entity.DishTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface DishTagMapper {
    // 新增标签
    int insert(DishTag tag);

    // 根据ID查询
    DishTag selectById(@Param("id") Long id);

    // 根据dishId查询所有标签
    List<DishTag> selectByDishId(@Param("dishId") Long dishId);

    // 根据标签类型和菜品ID查询
    List<DishTag> selectByDishIdAndType(@Param("dishId") Long dishId, @Param("tagType") String tagType);

    // 更新标签
    int update(DishTag tag);

    // 删除标签
    int deleteById(@Param("id") Long id);
} 
```

ai对话:

``` java
@Mapper
public interface AiConversationMapper {
    // 新增对话记录
    int insert(AiConversation conversation);

    // 根据ID查询
    AiConversation selectById(@Param("id") Long id);

    // 根据sessionId查询全部对话
    List<AiConversation> selectBySessionId(@Param("sessionId") String sessionId);

    // 查询用户所有对话
    List<AiConversation> selectByUserId(@Param("userId") Long userId);

    // 更新AI回复、上下文等
    int update(AiConversation conversation);

    // 删除对话记录
    int deleteById(@Param("id") Long id);
} 
```

Ai 推荐日志:

``` java
package com.star.mapper;

import com.star.entity.AiRecommendationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface AiRecommendationLogMapper {
    // 新增推荐记录
    int insert(AiRecommendationLog log);

    // 根据ID查询
    AiRecommendationLog selectById(@Param("id") Long id);

    // 根据sessionId查询推荐记录
    List<AiRecommendationLog> selectBySessionId(@Param("sessionId") String sessionId);

    // 查询用户所有推荐记录
    List<AiRecommendationLog> selectByUserId(@Param("userId") Long userId);

    // 更新推荐记录
    int update(AiRecommendationLog log);

    // 删除推荐记录
    int deleteById(@Param("id") Long id);
} 
```



##  AI 配置类

配置类，调用各类大模型需要

``` java
package com.star.ai.llm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import com.alibaba.dashscope.api.DashScopeClient;

/**
 * Spring AI大模型应用核心配置
 * 支持通义千问、DeepSeek等多模型，统一向量存储
 */
@Configuration
public class LLMApplicationConfig {

    /**
     * 通义千问API Key
     */
    @Value("${llm.dashscope.api-key:}")
    private String dashScopeApiKey;

    /**
     * DeepSeek API Key
     */
    @Value("${llm.deepseek.api-key:}")
    private String deepSeekApiKey;

    /**
     * Redis向量存储配置
     */
    @Bean
    public VectorStore vectorStore(RedisConnectionFactory redisConnectionFactory) {
        // 使用Spring AI官方Redis VectorStore实现
        return new RedisVectorStore(redisConnectionFactory, "star-food-ai-vector");
    }

    /**
     * ChatClient配置（可根据业务切换通义千问/DeepSeek）
     */
    @Bean
    public ChatClient chatClient() {
        // 这里只做示例，实际可根据配置动态切换
        // 推荐将DashScopeClient/DeepSeekClient封装为Spring AI ChatModel适配器
        return new ChatClient() {
            @Override
            public String call(String prompt) {
                // 伪代码：实际应调用DashScope/DeepSeek API
                // return dashScopeClient.chat(prompt, dashScopeApiKey);
                return "[AI回复示例] " + prompt;
            }
        };
    }

    /**
     * EmbeddingClient配置（用于文本向量化）
     */
    @Bean
    public EmbeddingClient embeddingClient() {
        // 伪代码：实际应调用DashScope/DeepSeek embedding API
        return new EmbeddingClient() {
            @Override
            public float[] embed(String text) {
                // return dashScopeClient.embed(text, dashScopeApiKey);
                return new float[768]; // 示例返回
            }
        };
    }
} 
```


