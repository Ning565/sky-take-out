package com.star.ai.chat;

import com.star.ai.agent.AgentCoordinator;
import com.star.pojo.vo.ChatResponse;
import com.star.ai.rag.RAGService;
import com.star.ai.llm.client.ModelServiceManager;
import com.star.pojo.dto.ChatRequest;
import com.star.result.Result;
import com.star.pojo.entity.AiConversation;
import com.star.pojo.entity.AiRecommendationLog;
import com.star.mapper.AiRecommendationLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;
import java.util.*;
import java.time.LocalDateTime;

/**
 * 对话接口控制器
 * 提供对话相关的API接口。
 */
@RestController
@RequestMapping("/api/ai")
@Slf4j
public class ChatController {
    
    @Autowired
    private ConversationService conversationService;
    
    @Autowired
    private RAGService ragService;
    
    @Autowired(required = false)
    private ModelServiceManager modelServiceManager;
    
    @Autowired
    private AiRecommendationLogMapper recommendationLogMapper;

    /**
     * AI智能对话接口 - 大模型应用核心
     */
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        try {
            log.info("接收到AI对话请求，用户ID：{}，会话ID：{}", request.getUserId(), request.getSessionId());
            
            // 参数验证
            if (!StringUtils.hasText(request.getMessage())) {
                return Result.error("消息内容不能为空");
            }
            
            if (request.getUserId() == null) {
                return Result.error("用户ID不能为空");
            }
            
            // 通过ConversationService处理对话
            ChatResponse response = conversationService.processMessage(request);
            
            log.info("AI对话处理完成，会话ID：{}", response.getSessionId());
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("AI对话处理失败", e);
            return Result.error("对话处理失败：" + e.getMessage());
        }
    }

    /**
     * RAG知识库搜索接口
     */
    @PostMapping("/search")
    public Result<List<String>> searchKnowledge(@RequestBody Map<String, Object> req) {
        try {
            String query = (String) req.getOrDefault("query", "");
            if (!StringUtils.hasText(query)) {
                return Result.error("查询内容不能为空");
            }
            
            int topK = (Integer) req.getOrDefault("topK", 5);
            double threshold = (Double) req.getOrDefault("threshold", 0.7);
            
            List<String> docs = ragService.searchKnowledge(query, topK, threshold);
            
            log.info("知识库搜索完成，查询：{}，结果数量：{}", query, docs.size());
            return Result.success(docs);
            
        } catch (Exception e) {
            log.error("知识库搜索失败", e);
            return Result.error("搜索失败：" + e.getMessage());
        }
    }

    /**
     * 多模型对比接口
     */
    @PostMapping("/compare")
    public Result<Map<String, String>> compareModels(@RequestBody Map<String, Object> req) {
        try {
            String prompt = (String) req.getOrDefault("prompt", "");
            if (!StringUtils.hasText(prompt)) {
                return Result.error("对比内容不能为空");
            }
            
            @SuppressWarnings("unchecked")
            List<String> modelIds = (List<String>) req.getOrDefault("modelIds", Arrays.asList("openai", "deepseek"));
            
            Map<String, String> results = new HashMap<>();
            
            if (modelServiceManager != null) {
                results = modelServiceManager.parallelCall(prompt, modelIds);
            } else {
                log.warn("ModelServiceManager未配置，返回模拟结果");
                results.put("openai", "OpenAI模型回复：" + prompt);
                results.put("deepseek", "DeepSeek模型回复：" + prompt);
            }
            
            log.info("多模型对比完成，模型数量：{}", results.size());
            return Result.success(results);
            
        } catch (Exception e) {
            log.error("多模型对比失败", e);
            return Result.error("对比失败：" + e.getMessage());
        }
    }

    /**
     * 推荐反馈接口
     */
    @PostMapping("/feedback")
    public Result<String> feedback(@RequestBody Map<String, Object> req) {
        try {
            String sessionId = (String) req.get("sessionId");
            Long userId = Long.valueOf(req.get("userId").toString());
            String feedbackType = (String) req.getOrDefault("feedbackType", "like"); // like, dislike, neutral
            String feedbackContent = (String) req.getOrDefault("feedbackContent", "");
            Long dishId = req.get("dishId") != null ? Long.valueOf(req.get("dishId").toString()) : null;
            
            // 记录推荐反馈日志
            AiRecommendationLog log = new AiRecommendationLog();
            log.setSessionId(sessionId);
            log.setUserId(userId);
            log.setDishId(dishId);
            log.setRecommendationReason("用户反馈");
            log.setUserFeedback(feedbackType + ":" + feedbackContent);
            log.setFeedbackScore(getFeedbackScore(feedbackType));
            log.setCreatedTime(LocalDateTime.now());
            log.setUpdatedTime(LocalDateTime.now());
            
            recommendationLogMapper.insert(log);
            
            log.info("用户反馈已记录，会话ID：{}，反馈类型：{}", sessionId, feedbackType);
            return Result.success("反馈已记录，谢谢您的建议！");
            
        } catch (Exception e) {
            log.error("记录推荐反馈失败", e);
            return Result.error("反馈记录失败：" + e.getMessage());
        }
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/conversation/{sessionId}")
    public Result<List<AiConversation>> getConversation(@PathVariable String sessionId) {
        try {
            if (!StringUtils.hasText(sessionId)) {
                return Result.error("会话ID不能为空");
            }
            
            List<AiConversation> conversations = conversationService.getConversationHistory(sessionId);
            
            log.info("查询对话历史完成，会话ID：{}，记录数量：{}", sessionId, conversations.size());
            return Result.success(conversations);
            
        } catch (Exception e) {
            log.error("查询对话历史失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取用户所有对话会话
     */
    @GetMapping("/conversations/user/{userId}")
    public Result<List<AiConversation>> getUserConversations(@PathVariable Long userId) {
        try {
            List<AiConversation> conversations = conversationService.getUserConversations(userId);
            
            log.info("查询用户对话历史完成，用户ID：{}，记录数量：{}", userId, conversations.size());
            return Result.success(conversations);
            
        } catch (Exception e) {
            log.error("查询用户对话历史失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取Agent执行链路追踪
     */
    @GetMapping("/agent-chain/{sessionId}")
    public Result<String> getAgentChain(@PathVariable String sessionId) {
        try {
            String agentChain = conversationService.getAgentChain(sessionId);
            return Result.success(agentChain);
        } catch (Exception e) {
            log.error("获取Agent链路失败", e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }
    
    /**
     * 将反馈类型转换为分数
     */
    private Double getFeedbackScore(String feedbackType) {
        switch (feedbackType.toLowerCase()) {
            case "like": return 1.0;
            case "dislike": return -1.0;
            case "neutral": return 0.0;
            default: return 0.0;
        }
    }
} 