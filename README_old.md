# star-food-chain 智能餐饮系统

## 项目简介
star-food-chain 是一套融合人工智能技术的新一代智能餐饮系统，基于大模型和AI Agent技术，为餐饮连锁企业提供智能化的点餐推荐、订单管理、员工管理、商品管理、优惠券、数据报表等全栈服务。系统采用微服务架构，集成RAG知识检索、多智能体协作、MCP协议等前沿AI技术，具备出色的智能化服务能力和高并发处理性能。

## 🤖 AI核心特性
- **智能美食推荐助手**：基于大模型的个性化菜品推荐系统
- **多轮对话交互**：自然语言理解用户需求，提供精准推荐
- **RAG知识增强**：融合菜品知识图谱，提供专业营养建议
- **多智能体协作**：推荐Agent、营养师Agent、客服Agent协同工作
- **MCP协议集成**：标准化模型控制和交互接口
- **实时推理优化**：毫秒级响应的AI推荐服务

## 主要功能
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

## 技术架构
- **后端框架**：Spring Boot
- **ORM 框架**：MyBatis
- **数据库**：MySQL
- **缓存**：Redis
- **消息队列**：RabbitMQ
- **分布式锁/限流**：Redisson
- **对象存储**：阿里云 OSS
- **实时通信**：WebSocket
- **其他**：Spring AOP、全局异常处理、分布式 ID 生成

## 目录结构说明
```
star-food-chain/
├── aliyun-oss-spring-boot-autoconfigure/   # 阿里云 OSS 自动配置模块
├── aliyun-oss-spring-boot-starter/        # 阿里云 OSS 启动器模块
├── star-common/                           # 公共工具与常量模块
├── star-pojo/                             # 实体、DTO、VO 等数据对象模块
├── star-server/                           # 业务主服务（控制器、服务、持久层等）
├── pom.xml                                # 项目聚合与依赖管理
└── README.md                              # 项目说明文档
```

## 快速启动
### 环境依赖
- JDK 8 及以上
- Maven 3.6+
- MySQL 5.7/8.0（需初始化数据库）
- Redis 5.0+
- RabbitMQ 3.8+
- 阿里云 OSS 账号与配置

### 启动步骤
1. 克隆项目到本地：
   ```bash
   git clone <项目地址>
   cd star-food-chain
   ```
2. 配置数据库、Redis、RabbitMQ、OSS 等连接信息（参考 `star-server/src/main/resources/application.yml`）。
3. 初始化数据库（可根据 `mapper/*.xml` 文件及实体类建表）。
4. 使用 Maven 构建项目：
   ```bash
   mvn clean install
   ```
5. 启动主服务：
   ```bash
   cd star-server
   mvn spring-boot:run
   ```
6. 访问接口文档或前端页面（如有）。

## 主要模块说明
- **star-server**：后端主服务，包含所有业务逻辑、控制器、服务、数据访问等。
- **star-pojo**：数据对象层，包含实体类、DTO、VO 等。
- **star-common**：公共工具类、常量、异常、通用返回对象等。
- **aliyun-oss-spring-boot-autoconfigure**：OSS 自动配置，便于集成阿里云对象存储。
- **aliyun-oss-spring-boot-starter**：OSS 启动器，简化 OSS 相关依赖和配置。

## 常见问题
1. **启动报错：数据库连接失败**
   - 检查 `application.yml` 中数据库配置是否正确，数据库服务是否已启动。
2. **Redis/RabbitMQ 连接异常**
   - 检查对应服务是否启动，配置项是否正确。
3. **阿里云 OSS 上传失败**
   - 检查 OSS 账号、密钥、Bucket 配置是否正确。
4. **端口冲突**
   - 检查本地 8080 端口是否被占用，可在配置文件中修改端口。
5. **数据库表不存在**
   - 请先初始化数据库表结构。

## 联系方式
如有问题或建议，请联系项目维护者：
- 邮箱：your_email@example.com
- 微信：your_wechat_id

---

> 本项目为学习与交流用途，欢迎提出宝贵意见和建议。 