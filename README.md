# star-food-chain 智能餐饮系统

## 项目简介
star-food-chain 是一套融合人工智能技术的新一代智能餐饮系统，基于大模型和AI Agent技术，为餐饮连锁企业提供智能化的点餐推荐、订单管理、员工管理、商品管理、优惠券、数据报表等全栈服务。系统采用微服务架构，集成RAG知识检索、多智能体协作、MCP协议等前沿AI技术，具备出色的智能化服务能力和高并发处理性能。

## 🤖 AI核心特性
- **智能美食推荐助手**：基于大模型的菜品推荐系统
- **多轮对话交互**：自然语言理解用户需求，提供精准推荐
- **多智能体协作**：专业分工的Agent协同处理用户请求
  - **理解Agent**：解析用户的用餐需求（人数、目的、口味偏好）
  - **推荐Agent**：基于理解结果匹配合适的菜品组合
  - **回答Agent**：整合推荐结果，生成自然语言回复
- **RAG知识检索**：基于菜品数据库的智能检索和匹配
- **简约推荐算法**：基于内容相似度的菜品推荐模型

## 主要功能

### 🍽️ 传统餐饮业务
- 门店点餐与下单
- 订单管理（下单、支付、取消、报表统计等）
- 员工管理（登录、权限、信息维护）
- 商品管理（菜品、套餐、分类、口味等）
- 购物车管理
- 优惠券与秒杀活动
- 数据统计与运营报表
- 地址簿管理
- 支付与通知（集成微信支付等）
- 文件上传（集成阿里云 OSS）
- 实时消息推送（WebSocket）

### 🧠 AI智能化功能
- **智能推荐对话机器人**：基于用户需求的菜品推荐
- **多轮对话系统**：自然语言交互，理解用餐人数、目的、口味偏好
- **多Agent协作系统**：
  - **理解Agent**：专门负责解析和提取用户的用餐需求信息
  - **推荐Agent**：根据理解结果执行菜品匹配和推荐算法
  - **回答Agent**：将推荐结果转化为自然、友好的回复
- **RAG知识检索**：基于菜品数据库的智能查询和匹配
- **推荐算法引擎**：基于内容相似度的菜品推荐模型
- **对话状态管理**：维护多轮对话的上下文信息

## 技术架构

### 🏗️ 基础架构
- **后端框架**：Spring Boot 3.x
- **ORM 框架**：MyBatis Plus
- **关系数据库**：MySQL 8.0
- **缓存**：Redis 7.x
- **消息队列**：RabbitMQ 3.x
- **分布式锁/限流**：Redisson
- **对象存储**：阿里云 OSS
- **实时通信**：WebSocket
- **其他**：Spring AOP、全局异常处理、分布式 ID 生成

### 🤖 AI技术栈
- **AI框架**：Spring AI
- **大语言模型**：OpenAI GPT / 通义千问（API调用）
- **Agent框架**：轻量级多Agent协作系统
- **推荐算法**：基于内容的推荐模型（余弦相似度匹配）
- **知识检索**：基于MySQL的RAG实现
- **对话管理**：Spring Boot集成的会话状态管理

## 目录结构说明
```
star-food-chain/
├── aliyun-oss-spring-boot-autoconfigure/   # 阿里云 OSS 自动配置模块
├── aliyun-oss-spring-boot-starter/        # 阿里云 OSS 启动器模块
├── star-common/                           # 公共工具与常量模块
├── star-pojo/                             # 实体、DTO、VO 等数据对象模块
├── star-server/                           # 业务主服务
│   ├── src/main/java/com/star/
│   │   ├── ai/                           # 🤖 AI模块
│   │   │   ├── agent/                    # 多智能体系统
│   │   │   │   ├── UnderstandingAgent.java       # 理解Agent - 解析用户需求
│   │   │   │   ├── RecommendationAgent.java      # 推荐Agent - 菜品匹配推荐
│   │   │   │   ├── ResponseAgent.java            # 回答Agent - 生成友好回复
│   │   │   │   └── AgentCoordinator.java         # Agent协调器 - 管理流程
│   │   │   ├── chat/                     # 对话系统
│   │   │   │   ├── ChatController.java           # 对话接口
│   │   │   │   ├── ConversationService.java      # 对话服务
│   │   │   │   └── ConversationContext.java      # 会话上下文
│   │   │   ├── recommendation/           # 推荐系统
│   │   │   │   ├── RecommendationEngine.java     # 推荐引擎
│   │   │   │   ├── ContentBasedFilter.java       # 基于内容推荐
│   │   │   │   └── DishMatcher.java              # 菜品匹配器
│   │   │   ├── rag/                      # RAG知识检索
│   │   │   │   ├── DishKnowledgeService.java     # 菜品知识服务
│   │   │   │   └── QueryProcessor.java           # 查询处理器
│   │   │   └── config/                   # AI配置
│   │   │       └── AIConfig.java                # AI统一配置
│   │   ├── controller/                   # 控制器层
│   │   ├── service/                      # 服务层
│   │   └── ...                          # 其他业务模块
├── pom.xml                               # 项目聚合与依赖管理
└── README.md                             # 项目说明文档
```

## 快速启动
### 环境依赖

#### 基础环境
- JDK 17 及以上
- Maven 3.8+
- MySQL 8.0（需初始化数据库）
- Redis 7.0+
- RabbitMQ 3.12+
- 阿里云 OSS 账号与配置

#### AI扩展环境
- **大模型API**：OpenAI API Key 或 通义千问 API Key
- **Java环境**：支持Spring AI框架

### 启动步骤

#### 基础服务启动
1. 克隆项目到本地：
   ```bash
   git clone <项目地址>
   cd star-food-chain
   ```
2. 配置基础服务连接信息（参考 `star-server/src/main/resources/application.yml`）
3. 初始化MySQL数据库（执行SQL脚本）
4. 使用 Maven 构建项目：
   ```bash
   mvn clean install
   ```

#### AI服务配置
5. 配置大模型API密钥（在application.yml中）：
   ```yaml
   spring:
     ai:
       openai:
         api-key: your_openai_api_key
       # 或者使用通义千问
       dashscope:
         api-key: your_dashscope_api_key
   ```

#### 服务启动
6. 启动主服务：
   ```bash
   cd star-server
   mvn spring-boot:run
   ```
7. 验证AI功能：
   ```bash
   # 测试智能推荐接口
   curl -X POST http://localhost:8080/api/ai/chat \
     -H "Content-Type: application/json" \
     -d '{"message": "我想要2个人吃辣一点的川菜"}'
   ```

## 主要模块说明

### 🏗️ 基础模块
- **star-server**：业务主服务，包含传统餐饮业务逻辑和AI智能化模块
- **star-pojo**：数据对象层，包含实体类、DTO、VO 等
- **star-common**：公共工具类、常量、异常、通用返回对象等
- **aliyun-oss-spring-boot-autoconfigure**：OSS 自动配置模块
- **aliyun-oss-spring-boot-starter**：OSS 启动器，简化依赖配置

### 🤖 AI核心模块
- **ai.agent**：多智能体系统（专业分工，简约协作）
  - `UnderstandingAgent`：理解Agent - 专门解析用户需求（人数、目的、口味）
  - `RecommendationAgent`：推荐Agent - 执行菜品匹配和推荐算法
  - `ResponseAgent`：回答Agent - 将推荐结果转化为自然语言回复
  - `AgentCoordinator`：协调器 - 管理三个Agent的协作流程
  
- **ai.chat**：对话系统
  - `ChatController`：对话REST接口
  - `ConversationService`：对话服务和多轮交互
  - `ConversationContext`：会话上下文管理
  
- **ai.recommendation**：推荐系统
  - `RecommendationEngine`：推荐引擎核心
  - `ContentBasedFilter`：基于内容的推荐算法
  - `DishMatcher`：菜品相似度匹配器
  
- **ai.rag**：知识检索系统
  - `DishKnowledgeService`：菜品知识库服务
  - `QueryProcessor`：查询处理和结果整合

## 常见问题

### 🛠️ 基础服务问题
1. **启动报错：数据库连接失败**
   - 检查 `application.yml` 中数据库配置是否正确，数据库服务是否已启动
2. **Redis/RabbitMQ 连接异常**
   - 检查对应服务是否启动，配置项是否正确
3. **阿里云 OSS 上传失败**
   - 检查 OSS 账号、密钥、Bucket 配置是否正确
4. **端口冲突**
   - 检查本地 8080 端口是否被占用，可在配置文件中修改端口
5. **数据库表不存在**
   - 请先初始化数据库表结构

### 🤖 AI服务问题
6. **大模型API调用失败**
   - 检查API密钥是否正确配置
   - 确认API配额和网络连接正常
   - 查看日志中的具体错误信息
7. **推荐结果不准确**
   - 检查菜品数据是否完整
   - 验证推荐算法参数设置
   - 确认用户需求解析是否正确
8. **多Agent协作异常**
   - 检查Agent配置和调用顺序
   - 查看Agent处理日志
   - 验证各Agent返回结果格式

### 🔧 性能优化
9. **AI响应速度慢**
   - 启用Redis缓存常用推荐结果
   - 优化数据库查询性能
   - 调整大模型调用参数

## 🎯 项目亮点与技术特色

### 💡 创新亮点
- **专业化多Agent协作**：理解Agent、推荐Agent、回答Agent各司其职
- **简约而强大的AI对话**：基于Spring AI的大模型集成
- **智能菜品推荐**：基于内容相似度的推荐算法
- **多轮对话状态管理**：维护完整的用户交互上下文

### 🏆 技术深度
- **多智能体系统**：清晰的Agent分工和协作流程
- **RAG知识检索**：基于现有数据库的智能查询增强
- **推荐算法**：余弦相似度匹配的内容推荐
- **对话管理**：Spring Boot集成的会话状态维护

### 📊 业务价值
- **提升用户体验**：智能对话推荐提高点餐效率
- **个性化服务**：基于用户需求的精准菜品匹配
- **降低人工成本**：AI助手减少客服人力投入
- **技术能力展示**：完整的AI应用开发案例

## 联系方式
如有问题或建议，请联系项目维护者：
- 邮箱：your_email@example.com
- 微信：your_wechat_id
- GitHub：https://github.com/your_username/star-food-chain

## 📜 许可证
本项目采用 [MIT License](LICENSE) 开源协议

---

> **⭐ 项目特色**：这是一个展示AI技术在餐饮行业应用的完整案例，涵盖大模型应用开发、多智能体协作、RAG知识检索、推荐算法等核心技术，代码简洁易懂，适合AI开发工程师学习和面试展示。

> **🚀 技术重点**：多Agent分工明确、Spring AI集成简洁、推荐算法实用、对话管理完善，是学习AI应用开发的优秀案例！ 


