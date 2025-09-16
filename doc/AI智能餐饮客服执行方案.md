# AI智能餐饮客服执行方案

## 📋 项目概述

### 项目目标

在现有star-food-chain餐饮系统基础上，增加AI智能客服问答功能，实现：

- 用户通过自然语言描述用餐需求（人数、目的、口味偏好）
- AI系统理解需求并推荐合适的菜品组合
- 提供友好的多轮对话体验

### 核心价值

- **技术展示**：展示多Agent协作、RAG检索、推荐算法等AI技术能力
- **用户体验**：提升点餐效率，减少选择困难
- **成本优化**：减少人工客服投入

### 技术原则

- **简约实用**：基于现有技术栈，避免过度工程化
- **模块化设计**：AI功能独立，不影响现有业务
- **易于维护**：代码结构清晰，便于理解和扩展

---

## 🏗️ 技术架构设计

### 整体架构图

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   用户前端      │───→│  Spring Boot    │───→│   AI服务层      │
│  (Web/Mobile)   │    │   Controller    │    │ (Agent System)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │                         │
                              ▼                         ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   MySQL数据库   │←───│  Service/Mapper │    │   大模型API     │
│ (菜品、订单等)  │    │      层         │    │ (OpenAI/通义)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 技术栈明细

**🧠 大模型应用核心**

- **AI框架**：Spring AI 1.0.0 + LangChain4j（混合使用）
- **大语言模型**：OpenAI GPT-4/3.5-turbo（主要）+ 通义千问（备用）
- **模型控制协议**：MCP (Model Control Protocol) 自研实现
- **向量化模型**：OpenAI text-embedding-ada-002
- **RAG框架**：Spring AI RAG + 自定义检索增强

**🏗️ 基础技术栈**

- **后端框架**：Spring Boot 3.2
- **数据库**：MySQL 8.0（现有）
- **缓存**：Redis 7.0（现有）+ 向量缓存
- **构建工具**：Maven 3.8

---

## 🗄️ 数据库设计扩展

### 新增表结构

#### 1. 对话记录表 (ai_conversation)

```sql
CREATE TABLE ai_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '对话ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    user_id BIGINT COMMENT '用户ID（可选）',
    user_message TEXT NOT NULL COMMENT '用户消息',
    ai_response TEXT NOT NULL COMMENT 'AI回复',
    conversation_context JSON COMMENT '对话上下文',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_time (created_time)
) COMMENT 'AI对话记录表';
```

#### 2. 菜品标签表 (dish_tags)

```sql
CREATE TABLE dish_tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    dish_id BIGINT NOT NULL COMMENT '菜品ID',
    tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
    tag_type VARCHAR(20) NOT NULL COMMENT '标签类型(taste/cuisine/feature)',
    weight DECIMAL(3,2) DEFAULT 1.00 COMMENT '权重',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_dish_id (dish_id),
    INDEX idx_tag_name (tag_name),
    INDEX idx_tag_type (tag_type),
    FOREIGN KEY (dish_id) REFERENCES dish(id)
) COMMENT '菜品标签表';
```

#### 3. 推荐记录表 (ai_recommendation_log)

```sql
CREATE TABLE ai_recommendation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '推荐ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    user_requirements JSON NOT NULL COMMENT '用户需求',
    recommended_dishes JSON NOT NULL COMMENT '推荐菜品列表',
    recommendation_score DECIMAL(3,2) COMMENT '推荐置信度',
    user_feedback TINYINT COMMENT '用户反馈(1:满意 0:不满意)',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id),
    INDEX idx_created_time (created_time)
) COMMENT 'AI推荐记录表';
```

### 现有表结构扩展

#### 菜品表(dish)字段扩展

```sql
ALTER TABLE dish ADD COLUMN description TEXT COMMENT '菜品详细描述';
ALTER TABLE dish ADD COLUMN nutrition_info JSON COMMENT '营养信息';
ALTER TABLE dish ADD COLUMN difficulty_level TINYINT DEFAULT 1 COMMENT '制作难度等级(1-5)';
ALTER TABLE dish ADD COLUMN recommend_count INT DEFAULT 0 COMMENT '被推荐次数';
```

---

## 🤖 AI模块详细设计

### 1. 大模型应用架构总览

#### 1.1 大模型应用开发核心流程

```
用户输入 → 多Agent协作 → RAG检索增强 → Function Calling（Tool调用）→ 智能回复
    ↓           ↓            ↓                ↓                ↓
  自然语言   → 结构化理解  → 知识增强   → 智能业务自动化  → 结果生成
```

#### 1.2 Spring AI框架集成策略

```java
@Configuration
@EnableSpringAI
public class LLMApplicationConfig {
    
    // 1. ChatClient - 大模型对话客户端
    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
            .defaultAdvisors(new SimpleLoggerAdvisor())
            .build();
    }
    
    // 2. EmbeddingClient - 向量化客户端
    @Bean
    public EmbeddingClient embeddingClient(OpenAiEmbeddingModel embeddingModel) {
        return EmbeddingClient.builder(embeddingModel).build();
    }
    
    // 3. VectorStore - 向量存储
    @Bean
    public VectorStore vectorStore(EmbeddingClient embeddingClient) {
        return new RedisVectorStore(embeddingClient);
    }
}
```

### 2. RAG检索增强生成系统（核心模块）

#### 2.1 RAG架构设计

RAG (Retrieval-Augmented Generation) 是让大模型"先查资料再回答"的核心技术：

```
用户问题 → 向量化 → 相似度检索 → 上下文增强 → 大模型生成 → 智能回复
    ↓        ↓        ↓         ↓         ↓        ↓
  "川菜推荐" → embedding → 查找川菜知识 → 注入菜品信息 → GPT生成 → "推荐麻婆豆腐..."
```

#### 2.2 Spring AI RAG实现

```java
@Service
@Slf4j
public class RAGService {
    
    @Autowired
    private ChatClient chatClient;
    
    @Autowired
    private VectorStore vectorStore;
    
    @Autowired
    private DishKnowledgeRepository dishRepository;
    
    /**
     * RAG核心方法：检索增强生成
     */
    public String ragGenerate(String userQuery, ConversationContext context) {
        // 1. 向量化用户查询
        List<Double> queryEmbedding = vectorStore.embed(userQuery);
        
        // 2. 相似度检索相关菜品知识
        List<Document> relevantDocs = vectorStore.similaritySearch(
            SearchRequest.query(userQuery)
                .withTopK(5)
                .withSimilarityThreshold(0.7)
        );
        
        // 3. 构建增强上下文
        String enhancedContext = buildEnhancedContext(relevantDocs, context);
        
        // 4. 大模型生成回复
        String ragPrompt = buildRAGPrompt(userQuery, enhancedContext);
        
        return chatClient.call(ragPrompt);
    }
    
    private String buildEnhancedContext(List<Document> docs, ConversationContext context) {
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("【菜品知识库】\n");
        
        for (Document doc : docs) {
            DishKnowledge dish = doc.getMetadata().get("dish", DishKnowledge.class);
            contextBuilder.append(String.format(
                "菜品：%s，价格：%.2f，口味：%s，菜系：%s，描述：%s\n",
                dish.getName(), dish.getPrice(), 
                dish.getTasteTag(), dish.getCuisineType(), dish.getDescription()
            ));
        }
        
        // 添加对话历史上下文
        if (context.getCurrentRequirement() != null) {
            contextBuilder.append("\n【用户偏好】\n");
            contextBuilder.append("人数：").append(context.getCurrentRequirement().getPeopleCount()).append("人\n");
            contextBuilder.append("口味偏好：").append(context.getCurrentRequirement().getTastePreferences()).append("\n");
        }
        
        return contextBuilder.toString();
    }
    
    private String buildRAGPrompt(String userQuery, String enhancedContext) {
        return String.format("""
            你是一个专业的餐饮推荐助手。请基于以下知识库信息回答用户问题。
            
            %s
            
            用户问题：%s
            
            要求：
            1. 必须基于知识库中的真实菜品信息进行推荐
            2. 不要编造不存在的菜品或价格
            3. 根据用户偏好进行个性化推荐
            4. 回复要自然友好，包含推荐理由
            5. 如果知识库中没有合适信息，诚实说明并引导用户
            
            回复：
            """, enhancedContext, userQuery);
    }
}
```

#### 2.3 知识库构建与向量化

```java
@Service
public class KnowledgeVectorService {
    
    @Autowired
    private VectorStore vectorStore;
    
    @Autowired
    private EmbeddingClient embeddingClient;
    
    @Autowired
    private DishMapper dishMapper;
    
    /**
     * 构建菜品知识向量库
     */
    @PostConstruct
    public void buildDishVectorDatabase() {
        log.info("开始构建菜品知识向量库...");
        
        // 1. 获取所有菜品数据
        List<Dish> allDishes = dishMapper.selectAll();
        
        // 2. 构建文档并向量化
        List<Document> documents = new ArrayList<>();
        
        for (Dish dish : allDishes) {
            // 构建富文本描述
            String richDescription = buildRichDescription(dish);
            
            // 创建文档
            Document document = new Document(richDescription);
            document.getMetadata().put("dishId", dish.getId());
            document.getMetadata().put("dishName", dish.getName());
            document.getMetadata().put("price", dish.getPrice());
            document.getMetadata().put("categoryId", dish.getCategoryId());
            
            documents.add(document);
        }
        
        // 3. 批量向量化并存储
        vectorStore.add(documents);
        
        log.info("菜品知识向量库构建完成，共处理{}道菜品", allDishes.size());
    }
    
    private String buildRichDescription(Dish dish) {
        // 获取菜品标签
        List<DishTag> tags = dishTagMapper.selectByDishId(dish.getId());
        
        StringBuilder description = new StringBuilder();
        description.append("菜品名称：").append(dish.getName()).append("\n");
        description.append("价格：").append(dish.getPrice()).append("元\n");
        description.append("描述：").append(dish.getDescription()).append("\n");
        
        // 按类型分组标签
        Map<String, List<String>> tagsByType = tags.stream()
            .collect(Collectors.groupingBy(
                DishTag::getTagType,
                Collectors.mapping(DishTag::getTagName, Collectors.toList())
            ));
        
        if (tagsByType.containsKey("taste")) {
            description.append("口味：").append(String.join("、", tagsByType.get("taste"))).append("\n");
        }
        if (tagsByType.containsKey("cuisine")) {
            description.append("菜系：").append(String.join("、", tagsByType.get("cuisine"))).append("\n");
        }
        if (tagsByType.containsKey("feature")) {
            description.append("特色：").append(String.join("、", tagsByType.get("feature"))).append("\n");
        }
        
        return description.toString();
    }
}
```

### 3. 多Agent协作系统（Spring AI集成）

#### 3.1 Agent架构与协作机制

```java
/**
 * Agent基础接口 - 基于Spring AI设计
 */
public interface AIAgent {
    
    /**
     * Agent处理逻辑
     */
    AgentResponse process(AgentRequest request, AgentContext context);
    
    /**
     * Agent能力描述（用于MCP协议）
     */
    AgentCapability getCapability();
    
    /**
     * Agent优先级
     */
    int getPriority();
}

/**
 * Agent上下文 - 包含大模型调用能力
 */
@Data
public class AgentContext {
    private String sessionId;
    private ConversationHistory history;
    private ChatClient chatClient;          // Spring AI ChatClient
    private VectorStore vectorStore;        // Spring AI VectorStore
    private Map<String, Object> sharedData; // Agent间共享数据
}
```

#### 3.2 UnderstandingAgent (理解Agent) - 大模型NLU

**职责**：使用大模型进行自然语言理解和需求提取

```java
@Component
@Slf4j
public class UnderstandingAgent implements AIAgent {
    
    @Autowired
    private ChatClient chatClient;
    
    @Override
    public AgentResponse process(AgentRequest request, AgentContext context) {
        String userMessage = request.getUserMessage();
        
        // 1. 构建结构化提取Prompt
        String extractionPrompt = buildExtractionPrompt(userMessage, context);
        
        // 2. 调用大模型进行结构化提取
        String llmResponse = chatClient.call(extractionPrompt);
        
        // 3. 解析大模型返回的JSON结构
        UserRequirement requirement = parseRequirement(llmResponse);
        
        // 4. 验证和标准化
        UserRequirement validatedRequirement = validateAndNormalize(requirement);
        
        // 5. 更新共享上下文
        context.getSharedData().put("userRequirement", validatedRequirement);
        
        return AgentResponse.success("需求理解完成", validatedRequirement);
    }
    
    private String buildExtractionPrompt(String userMessage, AgentContext context) {
        // 获取历史对话增强理解
        String conversationHistory = context.getHistory().getRecentMessages(3);
        
        return String.format("""
            你是一个专业的餐饮需求理解助手。请从用户消息中提取结构化的用餐需求信息。
            
            历史对话：
            %s
            
            当前用户消息：%s
            
            请提取以下信息并以JSON格式返回：
            {
                "peopleCount": "用餐人数（整数，如果未明确说明则为null）",
                "diningPurpose": "用餐目的（聚餐/商务/约会/家庭/快餐等）",
                "tastePreferences": ["口味偏好数组（辣/甜/清淡/重口/酸/麻等）"],
                "cuisineType": "菜系偏好（川菜/粤菜/湘菜/鲁菜/苏菜/浙菜/闽菜/徽菜/东北菜/西餐等）",
                "budgetRange": "预算范围（人均预算，整数，如100表示人均100元）",
                "dietaryRestrictions": ["饮食禁忌（素食/不吃辣/不吃海鲜/不吃牛肉等）"],
                "mealTime": "用餐时间（早餐/午餐/晚餐/夜宵）",
                "specialNeeds": ["特殊需求（聚餐/商务宴请/约会/孩子用餐等）"]
            }
            
            注意：
            1. 如果某个信息用户没有提到，对应字段设为null或空数组
            2. 尽量推理用户的隐含需求（如"两个人"推断为约会或朋友聚餐）
            3. 只返回JSON，不要其他解释文本
            
            JSON结果：
            """, conversationHistory, userMessage);
    }
    
    private UserRequirement parseRequirement(String llmResponse) {
        try {
            // 清理LLM响应，提取JSON部分
            String jsonPart = extractJSON(llmResponse);
            
            // 使用Jackson解析
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonPart, UserRequirement.class);
        } catch (Exception e) {
            log.error("解析用户需求失败：{}", llmResponse, e);
            return createDefaultRequirement();
        }
    }
    
    @Override
    public AgentCapability getCapability() {
        return AgentCapability.builder()
            .name("UnderstandingAgent")
            .description("自然语言理解和需求提取")
            .inputType("自然语言文本")
            .outputType("结构化用户需求")
            .build();
    }
}
```

#### 3.3 RecommendationAgent (推荐Agent) - RAG增强推荐

**职责**：基于RAG检索和大模型生成推荐结果

```java
@Component
@Slf4j
public class RecommendationAgent implements AIAgent {
    
    @Autowired
    private RAGService ragService;
    
    @Autowired
    private VectorStore vectorStore;
    
    @Autowired
    private ContentBasedFilter contentFilter;
    
    @Override
    public AgentResponse process(AgentRequest request, AgentContext context) {
        // 1. 获取理解Agent的结果
        UserRequirement requirement = (UserRequirement) context.getSharedData().get("userRequirement");
        
        if (requirement == null) {
            return AgentResponse.error("缺少用户需求信息");
        }
        
        // 2. 构建搜索查询
        String searchQuery = buildSearchQuery(requirement);
        
        // 3. RAG检索相关菜品
        List<Document> relevantDishes = vectorStore.similaritySearch(
            SearchRequest.query(searchQuery)
                .withTopK(10)
                .withSimilarityThreshold(0.6)
        );
        
        // 4. 基于内容过滤和排序
        List<DishRecommendation> filteredRecommendations = contentFilter.filterAndRank(
            relevantDishes, requirement
        );
        
        // 5. 使用大模型优化推荐理由
        List<DishRecommendation> enhancedRecommendations = enhanceWithLLM(
            filteredRecommendations, requirement, context
        );
        
        // 6. 构建最终推荐结果
        RecommendationResult result = RecommendationResult.builder()
            .dishes(enhancedRecommendations)
            .totalPrice(calculateTotalPrice(enhancedRecommendations))
            .confidenceScore(calculateConfidence(enhancedRecommendations))
            .build();
        
        // 7. 保存到共享上下文
        context.getSharedData().put("recommendationResult", result);
        
        return AgentResponse.success("推荐生成完成", result);
    }
    
    private String buildSearchQuery(UserRequirement requirement) {
        StringBuilder query = new StringBuilder();
        
        if (requirement.getCuisineType() != null) {
            query.append(requirement.getCuisineType()).append(" ");
        }
        
        if (requirement.getTastePreferences() != null && !requirement.getTastePreferences().isEmpty()) {
            query.append(String.join(" ", requirement.getTastePreferences())).append(" ");
        }
        
        if (requirement.getDiningPurpose() != null) {
            query.append(requirement.getDiningPurpose()).append(" ");
        }
        
        return query.toString().trim();
    }
    
    private List<DishRecommendation> enhanceWithLLM(
            List<DishRecommendation> recommendations, 
            UserRequirement requirement,
            AgentContext context) {
        
        for (DishRecommendation rec : recommendations) {
            // 使用大模型生成个性化推荐理由
            String reasonPrompt = String.format("""
                为以下菜品生成推荐理由：
                
                菜品：%s
                价格：%.2f元
                用户需求：%d人用餐，喜欢%s，偏好%s菜系
                
                请生成一句简洁的推荐理由（30字以内），说明为什么推荐这道菜：
                """, 
                rec.getDish().getName(),
                rec.getDish().getPrice(),
                requirement.getPeopleCount(),
                requirement.getTastePreferences(),
                requirement.getCuisineType()
            );
            
            String reason = context.getChatClient().call(reasonPrompt);
            rec.setReasonText(reason.trim());
        }
        
        return recommendations;
    }
}
```

#### 3.4 ResponseAgent (回答Agent) - 大模型自然语言生成

**职责**：将推荐结果转化为自然友好的对话回复

```java
@Component
@Slf4j
public class ResponseAgent implements AIAgent {
    
    @Autowired
    private ChatClient chatClient;
    
    @Override
    public AgentResponse process(AgentRequest request, AgentContext context) {
        // 1. 获取推荐结果
        RecommendationResult recommendation = (RecommendationResult) 
            context.getSharedData().get("recommendationResult");
        
        UserRequirement requirement = (UserRequirement) 
            context.getSharedData().get("userRequirement");
        
        if (recommendation == null || recommendation.getDishes().isEmpty()) {
            return generateNoResultsResponse(requirement, context);
        }
        
        // 2. 构建回复生成Prompt
        String responsePrompt = buildResponsePrompt(recommendation, requirement, context);
        
        // 3. 调用大模型生成自然语言回复
        String naturalResponse = chatClient.call(responsePrompt);
        
        // 4. 生成后续问题
        List<String> followUpQuestions = generateFollowUpQuestions(recommendation, requirement, context);
        
        // 5. 构建完整回复
        ChatResponse response = ChatResponse.builder()
            .messageText(naturalResponse)
            .recommendations(recommendation.getDishes())
            .followUpQuestions(followUpQuestions)
            .sessionId(context.getSessionId())
            .conversationState(ConversationState.RECOMMENDATION)
            .build();
        
        return AgentResponse.success("回复生成完成", response);
    }
    
    private String buildResponsePrompt(RecommendationResult recommendation, 
                                     UserRequirement requirement, 
                                     AgentContext context) {
        StringBuilder dishList = new StringBuilder();
        for (DishRecommendation dish : recommendation.getDishes()) {
            dishList.append(String.format("- %s（%.2f元）：%s\n", 
                dish.getDish().getName(), 
                dish.getDish().getPrice(),
                dish.getReasonText()
            ));
        }
        
        return String.format("""
            你是一个专业友好的餐厅服务员。请为客人介绍推荐的菜品。
            
            客人需求：%d人用餐，喜欢%s口味，偏好%s菜系
            
            推荐菜品：
            %s
            
            总价：%.2f元（人均%.2f元）
            
            要求：
            1. 语气要亲切自然，像真实服务员一样
            2. 简要介绍推荐的菜品特色
            3. 说明为什么适合客人的需求
            4. 提到总价和人均价格
            5. 整体回复控制在100字以内
            6. 不要使用"推荐指数"等生硬词汇
            
            回复：
            """, 
            requirement.getPeopleCount(),
            requirement.getTastePreferences(),
            requirement.getCuisineType(),
            dishList.toString(),
            recommendation.getTotalPrice(),
            recommendation.getTotalPrice().divide(BigDecimal.valueOf(requirement.getPeopleCount()), 2, RoundingMode.HALF_UP)
        );
    }
    
    private List<String> generateFollowUpQuestions(RecommendationResult recommendation,
                                                  UserRequirement requirement,
                                                  AgentContext context) {
        // 基于推荐结果和用户需求生成后续问题
        List<String> questions = new ArrayList<>();
        
        if (requirement.getBudgetRange() == null) {
            questions.add("对价格有什么要求吗？");
        }
        
        if (recommendation.getDishes().stream().noneMatch(d -> 
            d.getDish().getCategory().getName().contains("汤"))) {
            questions.add("需要配个汤吗？");
        }
        
        if (requirement.getDietaryRestrictions() == null || requirement.getDietaryRestrictions().isEmpty()) {
            questions.add("有什么忌口的吗？");
        }
        
        questions.add("这些菜品您觉得怎么样？");
        
        return questions.subList(0, Math.min(3, questions.size()));
    }
}
```

### 4. Function Calling（FC，函数/工具调用）模块设计

#### 4.1 业务场景下的典型Tool

- **菜谱/营养分析Tool（RecipeNutritionTool）**：对接外部菜谱/营养分析API，根据用户需求查询菜品信息、营养成分、做法等。

**举例：**
用户输入"我们三个人想吃辣的川菜，预算人均50，能不能帮我推荐菜品并分析营养？"

- 理解Agent提取结构化需求
- Function Calling Agent自动调用RecipeNutritionTool.queryRecipeAndNutrition，获取菜品推荐及营养分析
- 回复Agent告知推荐结果和营养信息

#### 4.2 技术架构与流程

```
用户输入 → Agent结构化理解 → 外部菜谱/营养分析API Tool（Function Calling）→ 智能回复
```

- 大模型通过Function Calling能力，自动识别用户意图，调用唯一的RecipeNutritionTool
- Tool执行后将结果反馈给大模型，由回复Agent生成最终回复

## 4.3 Tool接口设计示例

```java
// 菜谱/营养分析Tool接口
public class RecipeNutritionTool {
    public RecipeNutritionResult queryRecipeAndNutrition(UserRequirement req) {
        // 调用外部API，返回菜品推荐及营养分析
    }
}
```

#### 4.4 Function Calling在业务中的价值

- **智能自动化**：AI可根据用户意图自动推荐菜品并分析营养，无需人工干预。
- **业务闭环**：AI不仅能推荐，还能直接提供营养分析，提升用户体验。
- **工程简洁**：只需对接一个外部API，开发维护成本低。

#### 4.5 代码流程示例

```java
// 在Agent流程中集成Function Calling
if (userIntent.contains("推荐") || userIntent.contains("营养")) {
    RecipeNutritionResult result = recipeNutritionTool.queryRecipeAndNutrition(requirement);
    // 生成AI回复
    return "为您推荐菜品：" + result.getRecipeList() + "，营养分析：" + result.getNutritionInfo();
}
```

---

## 🔌 接口设计规范

### 1. REST API设计

#### 1.1 聊天对话接口

```http
POST /api/ai/chat
Content-Type: application/json

{
    "sessionId": "session_12345",
    "message": "我想要2个人吃辣一点的川菜",
    "userId": 10001
}
```

**响应格式**：

```json
{
    "code": 200,
    "message": "success",
    "data": {
        "sessionId": "session_12345",
        "response": "好的！为2位客人推荐几道经典川菜：麻婆豆腐、宫保鸡丁、水煮鱼片。这些菜品口感麻辣，很适合喜欢辣味的朋友。",
        "recommendations": [
            {
                "dishId": 1001,
                "dishName": "麻婆豆腐",
                "price": 28.00,
                "description": "经典川菜，豆腐嫩滑，麻辣鲜香",
                "matchScore": 0.95,
                "reason": "符合您的辣味偏好，适合2人份"
            }
        ],
        "followUpQuestions": [
            "需要搭配一些清淡的汤品吗？",
            "对预算有什么要求吗？"
        ],
        "conversationState": "RECOMMENDATION"
    }
}
```

#### 1.2 获取对话历史

```http
GET /api/ai/conversation/{sessionId}
```

#### 1.3 推荐反馈接口

```http
POST /api/ai/feedback
{
    "sessionId": "session_12345",
    "recommendationId": 1001,
    "feedback": 1,
    "comment": "推荐很好，已下单"
}
```

### 2. 内部服务接口

#### 2.1 Tool接口定义（Function Calling）

```java
public interface Tool {
    String getName();
    Object call(Map<String, Object> params);
}

// 具体实现
public class RecipeNutritionTool implements Tool {
    @Override
    public String getName() { return "RecipeNutritionTool"; }
    @Override
    public Object call(Map<String, Object> params) {
        // 调用外部API，返回菜品及营养分析
    }
}
```

#### 2.2 Agent与Tool协作

- Agent根据大模型输出的函数调用意图，自动调用唯一的RecipeNutritionTool
- Tool执行后将结果反馈给Agent，由回复Agent生成最终回复

---

## 📝 详细实施步骤

### 阶段一：基础框架搭建（3-4天）

#### Day 1: 大模型应用环境配置

1. **更新pom.xml添加大模型应用依赖**

```xml
<!-- Spring AI核心依赖 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-core</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 向量存储依赖 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-redis-store</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- LangChain4j集成（可选）-->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-spring-boot-starter</artifactId>
    <version>0.25.0</version>
</dependency>

<!-- JSON处理增强 -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>

<!-- HTTP客户端（MCP协议） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

2. **配置application.yml - 大模型应用配置**

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:your_api_key_here}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: gpt-3.5-turbo
          temperature: 0.7
          max-tokens: 1000
          top-p: 1.0
      embedding:
        options:
          model: text-embedding-ada-002
    
    # 通义千问配置（备用模型）
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:your_dashscope_key}
      chat:
        options:
          model: qwen-turbo
          temperature: 0.7

# 大模型应用专用配置
llm:
  # MCP协议配置
  mcp:
    server:
      port: 8081
      max-connections: 100
    client:
      timeout: 30s
      retry-count: 3
  
  # RAG配置
  rag:
    vector-store:
      type: redis
      similarity-threshold: 0.7
      max-results: 10
    embedding:
      batch-size: 10
      cache-ttl: 3600
  
  # Agent协作配置
  agents:
    understanding:
      model-preference: gpt-3.5-turbo
      timeout: 10s
    recommendation:
      model-preference: gpt-4
      timeout: 15s
    response:
      model-preference: gpt-3.5-turbo
      timeout: 8s

# AI对话配置
ai:
  conversation:
    session-timeout: 1800  # 30分钟
    max-history-size: 20
  recommendation:
    max-dishes-count: 10
    default-match-threshold: 0.6
```

#### Day 2: 数据库和向量存储设计

1. **执行SQL脚本创建AI相关表**
2. **配置Redis向量存储**

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 1  # 专门用于向量存储
```

3. **编写MyBatis Mapper接口和XML**
4. **创建AI相关的Entity和DTO类**

#### Day 3-4: 大模型应用框架搭建

1. **创建大模型应用包结构**

```
com.star.ai/
├── llm/                           # 大模型应用核心
│   ├── config/
│   │   ├── LLMApplicationConfig.java      # Spring AI配置
│   │   ├── MCPConfiguration.java          # MCP协议配置
│   │   └── VectorStoreConfig.java         # 向量存储配置
│   ├── client/
│   │   ├── MCPClient.java                 # MCP客户端
│   │   └── ModelServiceManager.java      # 模型服务管理
│   └── server/
│       ├── MCPServer.java                 # MCP服务器
│       └── ModelController.java           # 模型控制接口
├── agent/                         # 多Agent系统
│   ├── AgentCoordinator.java              # Agent协调器
│   ├── UnderstandingAgent.java            # 理解Agent
│   ├── RecommendationAgent.java           # 推荐Agent
│   ├── ResponseAgent.java                 # 回答Agent
│   └── base/
│       ├── AIAgent.java                   # Agent基础接口
│       ├── AgentContext.java              # Agent上下文
│       └── AgentCapability.java           # Agent能力描述
├── rag/                           # RAG检索增强
│   ├── RAGService.java                    # RAG核心服务
│   ├── KnowledgeVectorService.java        # 知识向量化
│   ├── VectorSearchService.java           # 向量检索
│   └── DocumentProcessor.java             # 文档处理
├── chat/                          # 对话管理
│   ├── ChatController.java                # 对话接口
│   ├── ConversationService.java           # 对话服务
│   ├── ConversationContext.java           # 对话上下文
│   └── MessageProcessor.java              # 消息处理
└── recommendation/                # 智能推荐
    ├── RecommendationEngine.java          # 推荐引擎
    ├── ContentBasedFilter.java            # 基于内容推荐
    └── DishMatcher.java                   # 菜品匹配
```

2. **实现Spring AI集成的基础类**

```java
@Configuration
@EnableSpringAI
public class LLMApplicationConfig {
    // ChatClient, EmbeddingClient, VectorStore配置
}
```

### 阶段二：大模型应用核心实现（5-6天）

#### Day 5-6: RAG检索增强系统实现

1. **Spring AI RAG核心服务**

```java
@Service
@Slf4j
public class RAGService {
    
    @Autowired
    private ChatClient chatClient;  // Spring AI ChatClient
    
    @Autowired
    private VectorStore vectorStore;  // Spring AI VectorStore
    
    /**
     * RAG核心：检索增强生成
     */
    public String ragGenerate(String userQuery, ConversationContext context) {
        // 1. 向量化查询
        List<Document> relevantDocs = vectorStore.similaritySearch(
            SearchRequest.query(userQuery)
                .withTopK(5)
                .withSimilarityThreshold(0.7)
        );
        
        // 2. 构建增强上下文
        String enhancedContext = buildEnhancedContext(relevantDocs, context);
        
        // 3. 大模型生成（检索增强）
        String ragPrompt = buildRAGPrompt(userQuery, enhancedContext);
        return chatClient.call(ragPrompt);
    }
}
```

2. **知识库向量化构建**

```java
@Service
public class KnowledgeVectorService {
    
    /**
     * 构建菜品知识向量库
     */
    @PostConstruct
    public void buildDishVectorDatabase() {
        List<Dish> allDishes = dishMapper.selectAll();
        List<Document> documents = new ArrayList<>();
        
        for (Dish dish : allDishes) {
            String richDescription = buildRichDescription(dish);
            Document document = new Document(richDescription);
            // 添加元数据
            document.getMetadata().put("dishId", dish.getId());
            documents.add(document);
        }
        
        // 批量向量化并存储到Redis
        vectorStore.add(documents);
    }
}
```

#### Day 7-8: 多Agent协作系统实现

1. **大模型驱动的UnderstandingAgent**

```java
@Component
@Slf4j
public class UnderstandingAgent implements AIAgent {
    
    @Autowired
    private ChatClient chatClient;  // 通过MCP调用大模型
    
    @Override
    public AgentResponse process(AgentRequest request, AgentContext context) {
        // 使用大模型进行自然语言理解
        String extractionPrompt = buildExtractionPrompt(request.getUserMessage(), context);
        String llmResponse = chatClient.call(extractionPrompt);
        
        // 解析大模型返回的结构化数据
        UserRequirement requirement = parseRequirement(llmResponse);
        
        // 保存到Agent间共享上下文
        context.getSharedData().put("userRequirement", requirement);
        
        return AgentResponse.success("需求理解完成", requirement);
    }
}
```

2. **RAG增强的RecommendationAgent**
3. **大模型生成的ResponseAgent**
4. **MCP集成的AgentCoordinator**

#### Day 9-10: Function Calling（Tool调用）集成

1. **定义标准Tool接口和常用业务Tool（OrderTool、CouponTool等）**
2. **在Agent流程中集成Function Calling能力**
3. **大模型输出函数调用意图时，自动路由到对应Tool并执行**
4. **将Tool执行结果反馈给大模型，由回复Agent生成最终回复**

### 阶段三：大模型应用接口层（3-4天）

#### Day 11-12: 大模型应用接口实现

1. **AI聊天Controller - 集成多Agent和RAG**

```java
@RestController
@RequestMapping("/api/ai")
@Slf4j
public class ChatController {
    
    @Autowired
    private AgentCoordinator agentCoordinator;  // 多Agent协调器
    
    @Autowired
    private ConversationService conversationService;
    
    @Autowired
    private MCPClient mcpClient;  // MCP协议客户端
    
    /**
     * AI智能对话接口 - 大模型应用核心
     */
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        try {
            log.info("收到AI对话请求: sessionId={}, message={}", 
                request.getSessionId(), request.getMessage());
            
            // 通过多Agent协作处理用户消息
            ChatResponse response = conversationService.processMessage(
                request.getSessionId(), 
                request.getMessage(), 
                request.getUserId()
            );
            
            log.info("AI对话处理完成: sessionId={}, recommendations={}", 
                request.getSessionId(), response.getRecommendations().size());
            
            return Result.success(response);
        } catch (Exception e) {
            log.error("AI对话处理失败", e);
            return Result.error("智能客服暂时不可用，请稍后再试");
        }
    }
    
    /**
     * RAG知识库搜索接口
     */
    @PostMapping("/search")
    public Result<List<Document>> searchKnowledge(@RequestBody SearchRequest request) {
        try {
            List<Document> results = ragService.searchKnowledge(
                request.getQuery(), 
                request.getTopK(), 
                request.getThreshold()
            );
            return Result.success(results);
        } catch (Exception e) {
            log.error("知识库搜索失败", e);
            return Result.error("知识库搜索暂时不可用");
        }
    }
    
    /**
     * 多模型对比接口（展示MCP能力）
     */
    @PostMapping("/compare")
    public Result<Map<String, String>> compareModels(@RequestBody CompareRequest request) {
        try {
            Map<String, String> results = new HashMap<>();
            
            // 并行调用多个模型
            List<String> modelIds = Arrays.asList("gpt-4", "gpt-3.5-turbo", "qwen-turbo");
            
            MCPRequest mcpRequest = MCPRequest.builder()
                .type(RequestType.CHAT)
                .content(request.getPrompt())
                .build();
            
            for (String modelId : modelIds) {
                mcpRequest.setModelId(modelId);
                MCPResponse response = mcpClient.sendRequest(mcpRequest);
                results.put(modelId, response.getContent());
            }
            
            return Result.success(results);
        } catch (Exception e) {
            log.error("模型对比失败", e);
            return Result.error("模型对比服务暂时不可用");
        }
    }
}
```

2. **ConversationService - 集成大模型应用能力**

```java
@Service
@Slf4j
public class ConversationServiceImpl implements ConversationService {
    
    @Autowired
    private AgentCoordinator agentCoordinator;  // 多Agent协调
    
    @Autowired
    private RAGService ragService;  // RAG检索增强
    
    @Autowired
    private MCPClient mcpClient;  // MCP模型调用
    
    @Override
    public ChatResponse processMessage(String sessionId, String message, Long userId) {
        log.info("开始处理大模型应用请求: sessionId={}", sessionId);
        
        try {
            // 1. 获取或创建对话上下文
            ConversationContext context = contextManager.getOrCreateContext(sessionId, userId);
            
            // 2. 通过多Agent协作处理消息（每个Agent都会调用大模型）
            ChatResponse response = agentCoordinator.process(message, context);
            
            // 3. 保存对话记录（包含大模型调用信息）
            saveConversationWithLLMInfo(sessionId, message, response);
            
            // 4. 更新对话上下文
            contextManager.updateContext(context);
            
            log.info("大模型应用处理完成: sessionId={}, agentCalls={}", 
                sessionId, response.getAgentCallCount());
            
            return response;
            
        } catch (Exception e) {
            log.error("大模型应用处理异常: sessionId=" + sessionId, e);
            return createFallbackResponse(sessionId, "抱歉，AI助手暂时不可用，请稍后再试。");
        }
    }
    
    private void saveConversationWithLLMInfo(String sessionId, String message, ChatResponse response) {
        AiConversation conversation = new AiConversation();
        conversation.setSessionId(sessionId);
        conversation.setUserMessage(message);
        conversation.setAiResponse(response.getMessageText());
        
        // 保存大模型调用信息
        Map<String, Object> llmInfo = new HashMap<>();
        llmInfo.put("modelCalls", response.getModelCalls());
        llmInfo.put("totalTokens", response.getTotalTokens());
        llmInfo.put("ragEnabled", response.isRagEnabled());
        llmInfo.put("agentChain", response.getAgentChain());
        
        conversation.setConversationContext(JSON.toJSONString(llmInfo));
        conversation.setCreatedTime(LocalDateTime.now());
        
        conversationMapper.insert(conversation);
    }
}
```

#### Day 13-14: 大模型应用监控与优化

1. **大模型调用监控**

```java
@Component
@Slf4j
public class LLMMetricsCollector {
    
    /**
     * 记录大模型调用指标
     */
    public void recordModelCall(String modelId, long responseTime, int tokenCount, boolean success) {
        // 记录响应时间
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("llm.call.duration")
            .tag("model", modelId)
            .tag("success", String.valueOf(success))
            .register(meterRegistry));
        
        // 记录Token使用量
        Gauge.builder("llm.tokens.used")
            .tag("model", modelId)
            .register(meterRegistry, tokenCount);
        
        // 记录成功率
        Counter.builder("llm.call.count")
            .tag("model", modelId)
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .increment();
    }
}
```

2. **大模型应用缓存策略**
3. **多模型负载均衡优化**

### 阶段四：大模型应用测试和优化（3-4天）

#### Day 15-16: 大模型应用测试

1. **RAG系统测试**

```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.ai.openai.api-key=test-key",
    "llm.rag.vector-store.type=memory"  // 测试用内存向量存储
})
class RAGServiceTest {
    
    @Autowired
    private RAGService ragService;
    
    @MockBean
    private VectorStore vectorStore;
    
    @Test
    void testRAGGenerate() {
        // 模拟向量检索结果
        List<Document> mockDocs = Arrays.asList(
            new Document("麻婆豆腐：经典川菜，麻辣鲜香", 
                Map.of("dishId", 1L, "cuisine", "川菜"))
        );
        
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenReturn(mockDocs);
        
        // 测试RAG生成
        String result = ragService.ragGenerate("推荐川菜", createMockContext());
        
        assertThat(result).isNotEmpty();
        assertThat(result).contains("麻婆豆腐");
        
        // 验证向量检索被调用
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }
}
```

2. **多Agent协作测试**

```java
@ExtendWith(MockitoExtension.class)
class AgentCoordinatorTest {
    
    @Mock
    private UnderstandingAgent understandingAgent;
    
    @Mock
    private RecommendationAgent recommendationAgent;
    
    @Mock
    private ResponseAgent responseAgent;
    
    @InjectMocks
    private AgentCoordinatorImpl agentCoordinator;
    
    @Test
    void testAgentPipeline() {
        // 模拟Agent链式调用
        when(understandingAgent.process(any(), any()))
            .thenReturn(AgentResponse.success("理解完成", mockRequirement));
        
        when(recommendationAgent.process(any(), any()))
            .thenReturn(AgentResponse.success("推荐完成", mockRecommendation));
        
        when(responseAgent.process(any(), any()))
            .thenReturn(AgentResponse.success("回复完成", mockChatResponse));
        
        // 测试完整流程
        ChatResponse result = agentCoordinator.process("2个人吃川菜", createMockContext());
        
        assertThat(result).isNotNull();
        assertThat(result.getRecommendations()).isNotEmpty();
        
        // 验证Agent调用顺序
        InOrder inOrder = inOrder(understandingAgent, recommendationAgent, responseAgent);
        inOrder.verify(understandingAgent).process(any(), any());
        inOrder.verify(recommendationAgent).process(any(), any());
        inOrder.verify(responseAgent).process(any(), any());
    }
}
```

3. **MCP协议测试**

```java
@Test
void testMCPClientFailover() {
    // 模拟第一个模型失败，第二个成功
    when(httpClient.post(contains("gpt-4")))
        .thenThrow(new RuntimeException("模型不可用"));
    
    when(httpClient.post(contains("gpt-3.5-turbo")))
        .thenReturn(createSuccessResponse());
    
    MCPRequest request = MCPRequest.builder()
        .content("测试消息")
        .build();
    
    // 测试故障转移
    MCPResponse response = mcpClient.parallelRequest(
        request, 
        Arrays.asList("gpt-4", "gpt-3.5-turbo")
    );
    
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.getModelId()).isEqualTo("gpt-3.5-turbo");
}
```

#### Day 17-18: 大模型应用性能优化

1. **大模型调用优化策略**

```java
@Service
@Slf4j
public class LLMOptimizationService {
    
    /**
     * 智能缓存策略 - 缓存相似查询结果
     */
    @Cacheable(value = "llm-cache", key = "#prompt.hashCode()")
    public String getCachedResponse(String prompt) {
        return mcpClient.sendRequest(MCPRequest.builder()
            .content(prompt)
            .build()).getContent();
    }
    
    /**
     * 批量处理优化 - 减少API调用次数
     */
    public List<String> batchProcess(List<String> prompts) {
        // 将多个prompt合并成一个批量请求
        String batchPrompt = prompts.stream()
            .map(p -> "Query: " + p)
            .collect(Collectors.joining("\n---\n"));
        
        String batchResponse = mcpClient.sendRequest(MCPRequest.builder()
            .content(batchPrompt)
            .build()).getContent();
        
        // 解析批量响应
        return parseBatchResponse(batchResponse);
    }
    
    /**
     * 异步处理 - 提高响应速度
     */
    @Async
    public CompletableFuture<String> asyncProcess(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            return mcpClient.sendRequest(MCPRequest.builder()
                .content(prompt)
                .build()).getContent();
        });
    }
}
```

2. **向量检索性能优化**

```java
@Service
public class VectorSearchOptimization {
    
    /**
     * 分层检索策略 - 先粗筛再精筛
     */
    public List<Document> hierarchicalSearch(String query) {
        // 第一层：快速粗筛（低相似度阈值，多结果）
        List<Document> roughResults = vectorStore.similaritySearch(
            SearchRequest.query(query)
                .withTopK(50)
                .withSimilarityThreshold(0.5)
        );
        
        // 第二层：精确筛选（高相似度阈值，少结果）
        return roughResults.stream()
            .filter(doc -> calculateExactSimilarity(query, doc) > 0.8)
            .limit(5)
            .collect(Collectors.toList());
    }
    
    /**
     * 预计算热门查询
     */
    @Scheduled(fixedRate = 3600000)  // 每小时执行
    public void precomputePopularQueries() {
        List<String> popularQueries = getPopularQueries();
        
        for (String query : popularQueries) {
            List<Document> results = vectorStore.similaritySearch(
                SearchRequest.query(query).withTopK(10)
            );
            
            // 缓存结果
            redisTemplate.opsForValue().set(
                "vector-cache:" + query, 
                results, 
                Duration.ofHours(24)
            );
        }
    }
}
```

3. **系统整体性能监控**

```java
@Component
public class PerformanceMonitor {
    
    @EventListener
    public void onLLMCall(LLMCallEvent event) {
        // 记录大模型调用性能
        metricsCollector.recordModelCall(
            event.getModelId(),
            event.getResponseTime(),
            event.getTokenCount(),
            event.isSuccess()
        );
        
        // 性能告警
        if (event.getResponseTime() > 5000) {  // 5秒
            alertService.sendSlowResponseAlert(event);
        }
    }
    
    @EventListener
    public void onRAGSearch(RAGSearchEvent event) {
        // 记录RAG检索性能
        metricsCollector.recordRAGSearch(
            event.getQuery(),
            event.getSearchTime(),
            event.getResultCount()
        );
    }
}
```

---

## 🧪 测试验证方案

### 1. 功能测试用例

#### 测试场景1：基础推荐功能

```
输入："我想要2个人吃辣一点的川菜"
期望输出：
- 正确识别人数：2
- 正确识别口味：辣
- 正确识别菜系：川菜
- 推荐3-4道川菜
- 总价格合理
```

#### 测试场景2：多轮对话

```
第一轮："我想吃川菜"
AI："好的，请问几位用餐？对辣度有什么要求吗？"

第二轮："2个人，要辣一点的"
AI："为您推荐麻婆豆腐、宫保鸡丁、水煮鱼片..."

第三轮："太贵了，便宜一点的"
AI："理解您的预算考虑，推荐回锅肉、麻辣豆腐..."
```

#### 测试场景3：异常处理

```
输入："今天天气真好"
期望：AI引导用户说明用餐需求

输入："dfasldkfj"
期望：AI提示无法理解，请重新描述
```

### 2. 性能测试指标

| 指标       | 目标值  | 测试方法         |
| ---------- | ------- | ---------------- |
| 响应时间   | < 3秒   | 接口压力测试     |
| 并发处理   | 100 TPS | JMeter压测       |
| 推荐准确率 | > 80%   | 人工评估         |
| 系统可用性 | 99.9%   | 24小时稳定性测试 |

### 3. 集成测试

1. **与现有订单系统集成**
2. **用户登录状态集成**
3. **支付流程集成**
4. **数据一致性验证**

---

## 🚀 部署配置指南

### 1. 环境变量配置

```bash
# 大模型API配置
export OPENAI_API_KEY="your_openai_api_key"
export DASHSCOPE_API_KEY="your_dashscope_api_key"

# 数据库配置
export MYSQL_URL="jdbc:mysql://localhost:3306/star_food_chain"
export MYSQL_USERNAME="root"
export MYSQL_PASSWORD="password"

# Redis配置
export REDIS_HOST="localhost"
export REDIS_PORT="6379"
export REDIS_PASSWORD=""
```

### 2. Docker配置（可选）

```dockerfile
FROM openjdk:17-jre-slim

COPY target/star-server-1.0.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### 3. 数据初始化脚本

```sql
-- 初始化菜品标签数据
INSERT INTO dish_tags (dish_id, tag_name, tag_type, weight) VALUES
(1, '辣', 'taste', 1.0),
(1, '川菜', 'cuisine', 1.0),
(1, '下饭', 'feature', 0.8),
(2, '甜', 'taste', 0.9),
(2, '粤菜', 'cuisine', 1.0);

-- 更新现有菜品描述
UPDATE dish SET description = '经典川菜，豆腐嫩滑，麻辣鲜香，是很受欢迎的下饭菜' WHERE name = '麻婆豆腐';
```

### 4. 监控配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

logging:
  level:
    com.star.ai: DEBUG
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/star-ai.log
```

---

## 📊 代码实现示例

### 1. 核心Service实现

```java
@Service
@Slf4j
public class ConversationServiceImpl implements ConversationService {
    
    @Autowired
    private AgentCoordinator agentCoordinator;
    
    @Autowired
    private ConversationContextManager contextManager;
    
    @Autowired
    private AiConversationMapper conversationMapper;
    
    @Override
    public ChatResponse processMessage(String sessionId, String message, Long userId) {
        log.info("Processing message for session: {}, message: {}", sessionId, message);
        
        try {
            // 1. 获取或创建对话上下文
            ConversationContext context = contextManager.getOrCreateContext(sessionId, userId);
            
            // 2. 保存用户消息
            saveUserMessage(sessionId, userId, message);
            
            // 3. 通过Agent协调器处理消息
            ChatResponse response = agentCoordinator.process(message, context);
            
            // 4. 保存AI回复
            saveAiResponse(sessionId, response);
            
            // 5. 更新对话上下文
            contextManager.updateContext(context);
            
            log.info("Successfully processed message for session: {}", sessionId);
            return response;
            
        } catch (Exception e) {
            log.error("Error processing message for session: " + sessionId, e);
            return createErrorResponse(sessionId, "抱歉，我暂时无法理解您的需求，请稍后再试。");
        }
    }
    
    private void saveUserMessage(String sessionId, Long userId, String message) {
        AiConversation conversation = new AiConversation();
        conversation.setSessionId(sessionId);
        conversation.setUserId(userId);
        conversation.setUserMessage(message);
        conversation.setCreatedTime(LocalDateTime.now());
        
        conversationMapper.insert(conversation);
    }
}
```

### 2. Agent协调器实现

```java
@Component
@Slf4j
public class AgentCoordinatorImpl implements AgentCoordinator {
    
    @Autowired
    private UnderstandingAgent understandingAgent;
    
    @Autowired
    private RecommendationAgent recommendationAgent;
    
    @Autowired
    private ResponseAgent responseAgent;
    
    @Override
    public ChatResponse process(String userMessage, ConversationContext context) {
        log.debug("Agent coordinator processing message: {}", userMessage);
        
        try {
            // 1. 理解用户需求
            UserRequirement requirement = understandingAgent.extractRequirements(userMessage, context);
            log.debug("Extracted requirement: {}", requirement);
            
            // 2. 执行推荐算法
            RecommendationResult recommendationResult = recommendationAgent.recommend(requirement, context);
            log.debug("Recommendation result: {} dishes", recommendationResult.getDishes().size());
            
            // 3. 生成友好回复
            ChatResponse response = responseAgent.generateResponse(recommendationResult, context);
            log.debug("Generated response with {} recommendations", response.getDishCards().size());
            
            // 4. 更新对话状态
            updateConversationState(context, requirement, recommendationResult);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error in agent coordination", e);
            throw new AIServiceException("AI服务处理异常", e);
        }
    }
    
    private void updateConversationState(ConversationContext context, 
                                       UserRequirement requirement, 
                                       RecommendationResult result) {
        context.setCurrentRequirement(requirement);
        context.setLastActiveTime(LocalDateTime.now());
        
        if (result.getDishes().isEmpty()) {
            context.setState(ConversationState.CLARIFICATION);
        } else {
            context.setState(ConversationState.RECOMMENDATION);
        }
    }
}
```

---

## 📈 项目验收标准

### 功能验收

- [ ] 用户可以通过自然语言描述需求
- [ ] AI能正确理解用餐人数、口味、菜系等信息
- [ ] 系统能推荐合适的菜品组合
- [ ] 支持多轮对话和需求澄清
- [ ] 推荐结果包含价格、描述、理由
- [ ] 能够处理异常输入和边界情况

### 技术验收

- [ ] 代码结构清晰，模块划分合理
- [ ] 单元测试覆盖率达到80%以上
- [ ] 接口响应时间小于3秒
- [ ] 支持并发访问，无数据竞争
- [ ] 日志记录完整，便于调试
- [ ] 配置灵活，支持多环境部署

### 业务验收

- [ ] 推荐准确率达到80%以上
- [ ] 用户满意度调查结果良好
- [ ] 能够有效引导用户下单
- [ ] 降低客服人工成本
- [ ] 提升用户点餐体验

---

## 