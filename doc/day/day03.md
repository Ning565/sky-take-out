# 一、Function Calling（FC，函数/工具调用）模块设计

## 1.业务场景下的典型Tool

- **菜谱/营养分析Tool（RecipeNutritionTool）**：对接外部菜谱/营养分析API，根据用户需求查询菜品信息、营养成分、做法等。

**举例：**
用户输入"我们三个人想吃辣的川菜，预算人均50，能不能帮我推荐菜品并分析营养？"

- 理解Agent提取结构化需求
- Function Calling Agent自动调用RecipeNutritionTool.queryRecipeAndNutrition，获取菜品推荐及营养分析
- 回复Agent告知推荐结果和营养信息

## 2. 技术架构与流程

```
用户输入 → Agent结构化理解 → 外部菜谱/营养分析API Tool（Function Calling）→ 智能回复
```

- 大模型通过Function Calling能力，自动识别用户意图，调用唯一的RecipeNutritionTool
- Tool执行后将结果反馈给大模型，由回复Agent生成最终回复

## 3. Tool接口设计示例

```java
// 菜谱/营养分析Tool接口
public class RecipeNutritionTool {
    public RecipeNutritionResult queryRecipeAndNutrition(UserRequirement req) {
        // 调用外部API，返回菜品推荐及营养分析
    }
}
```

## 4. Function Calling在业务中的价值

- **智能自动化**：AI可根据用户意图自动推荐菜品并分析营养，无需人工干预。
- **业务闭环**：AI不仅能推荐，还能直接提供营养分析，提升用户体验。
- **工程简洁**：只需对接一个外部API，开发维护成本低。

## 5. 代码流程示例(要补全实现所有的代码流程)

```java
// 在Agent流程中集成Function Calling
if (userIntent.contains("推荐") || userIntent.contains("营养")) {
    RecipeNutritionResult result = recipeNutritionTool.queryRecipeAndNutrition(requirement);
    // 生成AI回复
    return "为您推荐菜品：" + result.getRecipeList() + "，营养分析：" + result.getNutritionInfo();
}
```



# 二、 Agent与Tool协作

- Agent根据大模型输出的函数调用意图，自动调用唯一的RecipeNutritionTool
- Tool执行后将结果反馈给Agent，由回复Agent生成最终回复

##Function Calling（Tool调用）集成

1. **定义标准Tool接口和常用业务Tool（OrderTool、CouponTool等）**
2. **在Agent流程中集成Function Calling能力**
3. **大模型输出函数调用意图时，自动路由到对应Tool并执行**
4. **将Tool执行结果反馈给大模型，由回复Agent生成最终回复**


# 三、大模型应用接口层，实现各类交互的接口

## 1.  大模型应用接口实现

 **AI聊天Controller - 集成多Agent和RAG**

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

# 四、新增的AI功能的各类接口设计规范

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

```
# 五、核心service实现，实现若干核心的service能力，新增的ai功能的service**ConversationService - 集成大模型应用能力**

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

