# day02 大模型应用框架搭建


## 1.2实现Spring AI集成的基础类/配置类

``` java
@Configuration

@EnableSpringAI

public class LLMApplicationConfig {

	*// ChatClient, EmbeddingClient, VectorStore配置*

}
```

## 2实现RAG检索增强系统

2.0 **Spring AI RAG核心服务**

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

**3. 文档处理**

DocumentProcessor.java ，上下文增强，大模型生成，智能回复



**整体RAG执行框架：**

```
用户问题 → 向量化 → 相似度检索 → 上下文增强 → 大模型生成 → 智能回复

​    ↓        ↓        ↓         ↓         ↓        ↓

  "川菜推荐" → embedding → 查找川菜知识 → 注入菜品信息 → GPT生成 → "推荐麻婆豆腐..."
```

## 搭建多Agent协作系统

搭建Agent基本接口，上下文模型调用能力

实现：UnderstandingAgent (理解Agent) - 大模型NLU、RecommendationAgent (推荐Agent) - RAG增强推荐和ResponseAgent (回答Agent) - 大模型自然语言生成

多Agent协作系统实现
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
-  **RAG增强的RecommendationAgent**
-  **大模型生成的ResponseAgent**
-  **Function calling集成的AgentCoordinator**
### 