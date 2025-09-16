package com.star.ai.chat;

import com.star.ai.agent.AgentCoordinator;
import com.star.pojo.vo.ChatResponse;
import com.star.pojo.dto.ChatRequest;
import com.star.result.Result;
import com.star.ai.rag.RAGService;
import com.star.ai.llm.client.ModelServiceManager;
import com.star.mapper.AiConversationMapper;
import com.star.pojo.entity.AiConversation;
import com.star.ai.agent.base.AgentRequest;
import com.star.ai.agent.base.AgentContext;
import com.star.ai.agent.base.AgentResponse;
import com.star.ai.agent.base.ConversationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;

/**
 * 对话服务
 * 负责对话管理与上下文维护。
 */
@Service
@Slf4j
public class ConversationService {
    @Autowired
    private AgentCoordinator agentCoordinator;
    @Autowired
    private RAGService ragService;
    @Autowired(required = false)
    private ModelServiceManager modelServiceManager;
    @Autowired
    private AiConversationMapper conversationMapper;
    @Value("${ai.conversation.history.max:10}")
    private int maxHistory;
    // TODO: 注入对话上下文管理、对话记录等

    /**
     * 处理AI对话主流程
     * @param request 对话请求
     * @return 对话回复
     */
    @Transactional
    public ChatResponse processMessage(ChatRequest request) {
        // 1. 获取或创建对话上下文
        String sessionId = request.getSessionId();
        Long userId = request.getUserId();
        List<AiConversation> historyList = StringUtils.hasText(sessionId) ? conversationMapper.selectBySessionId(sessionId) : Collections.emptyList();
        List<ConversationMessage> history = new ArrayList<>();
        for (AiConversation conv : historyList) {
            history.add(ConversationMessage.userMessage(conv.getUserMessage()));
            history.add(ConversationMessage.assistantMessage(conv.getAiResponse()));
        }
        // 2. 构建Agent上下文
        AgentContext agentContext = AgentContext.builder()
                .sessionId(sessionId)
                .history(history)
                .sharedData(new HashMap<>())
                .build();
        // 3. 构建Agent请求
        AgentRequest agentRequest = AgentRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .userMessage(request.getMessage())
                .requestType("chat")
                .parameters(Map.of("userId", userId))
                .build();
        // 4. 多Agent链路处理
        AgentResponse agentResponse = agentCoordinator.coordinate(agentRequest, agentContext);
        // 5. 记录对话历史
        AiConversation conversation = new AiConversation();
        conversation.setSessionId(sessionId);
        conversation.setUserId(userId);
        conversation.setUserMessage(request.getMessage());
        conversation.setAiResponse(agentResponse.getMessage());
        conversation.setConversationContext(agentContext.getSharedData() != null ? agentContext.getSharedData().toString() : null);
        conversation.setConversationState("RECOMMENDATION");
        conversation.setModelInfo(null); // 可补充模型调用信息
        conversation.setResponseTime(null); // 可补充响应时间
        conversation.setTokenUsage(null); // 可补充token信息
        conversation.setCreatedTime(LocalDateTime.now());
        conversation.setUpdatedTime(LocalDateTime.now());
        conversationMapper.insert(conversation);
        // 6. 返回结构化回复
        Object data = agentResponse.getData();
        ChatResponse chatResponse = data instanceof ChatResponse ? (ChatResponse) data : new ChatResponse();
        chatResponse.setSessionId(sessionId);
        chatResponse.setConversationState("RECOMMENDATION");
        return chatResponse;
    }

    /**
     * 查询对话历史
     */
    public List<AiConversation> getConversationHistory(String sessionId) {
        return conversationMapper.selectBySessionId(sessionId);
    }

    /**
     * 查询用户所有对话
     */
    public List<AiConversation> getUserConversations(Long userId) {
        return conversationMapper.selectByUserId(userId);
    }

    /**
     * 查询Agent链路追踪信息（示例：返回上下文中的agentChain字段）
     */
    public String getAgentChain(String sessionId) {
        List<AiConversation> list = conversationMapper.selectBySessionId(sessionId);
        if (list.isEmpty()) return null;
        // 假设agentChain信息保存在conversationContext或modelInfo字段
        return list.get(list.size() - 1).getConversationContext();
    }
} 