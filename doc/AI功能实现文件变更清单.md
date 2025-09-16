# AI智能餐饮客服功能实现 - 目录结构变更清单（树状结构）

```
star-food-chain/
├── pom.xml  # [修改] 根依赖
├── star-server/
│   ├── pom.xml  # [修改]
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── star/
│   │   │   │           ├── ai/
│   │   │   │           │   ├── llm/
│   │   │   │           │   │   ├── config/
│   │   │   │           │   │   │   ├── LLMApplicationConfig.java
│   │   │   │           │   │   │   ├── MCPConfiguration.java
│   │   │   │           │   │   │   └── VectorStoreConfig.java
│   │   │   │           │   │   ├── client/
│   │   │   │           │   │   │   ├── MCPClient.java
│   │   │   │           │   │   │   ├── ModelServiceManager.java
│   │   │   │           │   │   │   └── LoadBalancer.java
│   │   │   │           │   │   ├── server/
│   │   │   │           │   │   │   ├── MCPServer.java
│   │   │   │           │   │   │   └── ModelController.java
│   │   │   │           │   ├── agent/
│   │   │   │           │   │   ├── base/
│   │   │   │           │   │   │   ├── AIAgent.java
│   │   │   │           │   │   │   ├── AgentContext.java
│   │   │   │           │   │   │   ├── AgentCapability.java
│   │   │   │           │   │   │   ├── AgentRequest.java
│   │   │   │           │   │   │   └── AgentResponse.java
│   │   │   │           │   │   ├── AgentCoordinator.java
│   │   │   │           │   │   ├── AgentCoordinatorImpl.java
│   │   │   │           │   │   ├── UnderstandingAgent.java
│   │   │   │           │   │   ├── RecommendationAgent.java
│   │   │   │           │   │   └── ResponseAgent.java
│   │   │   │           │   ├── rag/
│   │   │   │           │   │   ├── RAGService.java
│   │   │   │           │   │   ├── KnowledgeVectorService.java
│   │   │   │           │   │   ├── VectorSearchService.java
│   │   │   │           │   │   ├── DocumentProcessor.java
│   │   │   │           │   │   └── EmbeddingService.java
│   │   │   │           │   ├── chat/
│   │   │   │           │   │   ├── ChatController.java
│   │   │   │           │   │   ├── ConversationService.java  # [修改]
│   │   │   │           │   │   ├── ConversationContext.java
│   │   │   │           │   │   ├── ConversationContextManager.java
│   │   │   │           │   │   └── MessageProcessor.java
│   │   │   │           │   ├── recommendation/
│   │   │   │           │   │   ├── RecommendationEngine.java
│   │   │   │           │   │   ├── ContentBasedFilter.java
│   │   │   │           │   │   └── DishMatcher.java
│   │   │   │           │   ├── monitor/
│   │   │   │           │   │   ├── LLMMetricsCollector.java
│   │   │   │           │   │   ├── PerformanceMonitor.java
│   │   │   │           │   │   └── AIHealthIndicator.java
│   │   │   │           │   └── utils/
│   │   │   │           │       ├── PromptTemplateUtil.java
│   │   │   │           │       └── VectorUtil.java
│   │   │   │           ├── mapper/
│   │   │   │           │   ├── AiConversationMapper.java
│   │   │   │           │   ├── DishTagMapper.java
│   │   │   │           │   └── AiRecommendationLogMapper.java
│   │   │   │           └── ...（原有业务代码）
│   │   │   ├── resources/
│   │   │   │   ├── application.yml  # [修改]
│   │   │   │   ├── application-dev.yml  # [修改]
│   │   │   │   ├── ai-prompts.yml
│   │   │   │   ├── mcp-config.yml
│   │   │   │   ├── sql/
│   │   │   │   │   ├── ai_tables.sql
│   │   │   │   │   ├── ai_init_data.sql
│   │   │   │   │   └── ai_indexes.sql
│   │   │   │   └── mapper/
│   │   │   │       ├── AiConversationMapper.xml
│   │   │   │       ├── DishTagMapper.xml
│   │   │   │       └── AiRecommendationLogMapper.xml
│   │   └── test/
│   │       └── java/
│   │           └── com/
│   │               └── star/
│   │                   └── ai/
│   │                       ├── RAGServiceTest.java
│   │                       ├── AgentCoordinatorTest.java
│   │                       ├── UnderstandingAgentTest.java
│   │                       ├── MCPClientTest.java
│   │                       ├── ChatControllerTest.java
│   │                       └── AIIntegrationTest.java
│   ├── docker/
│   │   ├── ai-services/
│   │   │   └── Dockerfile
│   │   └── docker-compose-ai.yml
│   └── scripts/
│       ├── init-vector-db.sh
│       └── deploy-ai.sh
├── star-pojo/
│   └── src/
│       └── main/
│           └── java/
│               └── com/
│                   └── star/
│                       ├── entity/
│                       │   ├── AiConversation.java
│                       │   ├── DishTag.java
│                       │   └── AiRecommendationLog.java
│                       ├── dto/
│                       │   ├── ChatRequest.java
│                       │   ├── UserRequirement.java
│                       │   ├── SearchRequest.java
│                       │   ├── MCPRequest.java
│                       │   └── MCPResponse.java
│                       └── vo/
│                           ├── ChatResponse.java
│                           ├── DishRecommendation.java
│                           └── RecommendationResult.java
└── star-common/
    └── src/
        └── main/
            └── java/
                └── com/
                    └── star/
                        └── utils/
                            ├── PromptTemplateUtil.java
                            └── VectorUtil.java
```

---

**说明：**
- [新增] 表示新建文件或目录；[修改] 表示对现有文件进行修改或扩展。
- 目录结构已按AI模块、数据层、配置、测试、部署等分层归类，便于开发和维护。
- 只列出与AI智能餐饮客服相关的新增/变更部分，原有业务代码省略。 