package com.star.ai.chat;

import com.star.ai.agent.base.ConversationMessage;
import com.star.pojo.dto.ChatRequest;
import com.star.pojo.vo.ChatResponse;  
import com.star.pojo.dto.UserRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 消息处理器
 * 负责对话消息的预处理、分类、分发和后处理。
 */
@Component
@Slf4j
public class MessageProcessor {
    
    /**
     * 消息类型枚举
     */
    public enum MessageType {
        /**
         * 需求描述类消息（用户描述用餐需求）
         */
        REQUIREMENT_DESCRIPTION,
        
        /**
         * 确认类消息（用户确认推荐）
         */
        CONFIRMATION,
        
        /**
         * 询问类消息（用户询问菜品详情）
         */
        INQUIRY,
        
        /**
         * 反馈类消息（用户提供反馈）
         */
        FEEDBACK,
        
        /**
         * 修改类消息（用户要求修改推荐）
         */
        MODIFICATION,
        
        /**
         * 问候类消息（打招呼等）
         */
        GREETING,
        
        /**
         * 其他类消息
         */
        OTHER
    }
    
    /**
     * 情感分析结果
     */
    public enum SentimentType {
        POSITIVE, NEGATIVE, NEUTRAL
    }
    
    private static final Map<String, MessageType> INTENT_PATTERNS = new HashMap<>();
    private static final Map<String, List<String>> SENTIMENT_KEYWORDS = new HashMap<>();
    
    static {
        // 初始化意图识别模式
        initIntentPatterns();
        // 初始化情感关键词
        initSentimentKeywords();
    }
    
    /**
     * 预处理消息
     */
    public String preprocessMessage(String rawMessage) {
        if (!StringUtils.hasText(rawMessage)) {
            return "";
        }
        
        // 1. 去除多余空格和换行
        String processed = rawMessage.trim().replaceAll("\\s+", " ");
        
        // 2. 表情符号处理（简单替换）
        processed = processed.replaceAll("[😊😄😃😀🙂]", "[开心]");
        processed = processed.replaceAll("[😔😞😟😢]", "[难过]");
        processed = processed.replaceAll("[👍👌]", "[满意]");
        processed = processed.replaceAll("[👎]", "[不满意]");
        
        // 3. 数字标准化
        processed = processed.replaceAll("一", "1");
        processed = processed.replaceAll("二|两", "2");
        processed = processed.replaceAll("三", "3");
        processed = processed.replaceAll("四", "4");
        processed = processed.replaceAll("五", "5");
        processed = processed.replaceAll("六", "6");
        processed = processed.replaceAll("七", "7");
        processed = processed.replaceAll("八", "8");
        processed = processed.replaceAll("九", "9");
        processed = processed.replaceAll("十", "10");
        
        log.debug("消息预处理：{} -> {}", rawMessage, processed);
        return processed;
    }
    
    /**
     * 分析消息类型
     */
    public MessageType analyzeMessageType(String message) {
        String lowerMessage = message.toLowerCase();
        
        // 检查各种意图模式
        for (Map.Entry<String, MessageType> entry : INTENT_PATTERNS.entrySet()) {
            String pattern = entry.getKey();
            MessageType type = entry.getValue();
            
            if (Pattern.compile(pattern).matcher(lowerMessage).find()) {
                log.debug("识别消息类型：{} -> {}", message, type);
                return type;
            }
        }
        
        return MessageType.OTHER;
    }
    
    /**
     * 提取关键信息
     */
    public Map<String, Object> extractKeyInformation(String message) {
        Map<String, Object> keyInfo = new HashMap<>();
        
        // 1. 提取人数信息
        Integer peopleCount = extractPeopleCount(message);
        if (peopleCount != null) {
            keyInfo.put("peopleCount", peopleCount);
        }
        
        // 2. 提取预算信息
        Integer budget = extractBudget(message);
        if (budget != null) {
            keyInfo.put("budget", budget);
        }
        
        // 3. 提取口味偏好
        List<String> tastes = extractTastePreferences(message);
        if (!tastes.isEmpty()) {
            keyInfo.put("tastePreferences", tastes);
        }
        
        // 4. 提取菜系偏好
        String cuisine = extractCuisineType(message);
        if (cuisine != null) {
            keyInfo.put("cuisineType", cuisine);
        }
        
        // 5. 提取用餐目的
        String purpose = extractDiningPurpose(message);
        if (purpose != null) {
            keyInfo.put("diningPurpose", purpose);
        }
        
        // 6. 提取饮食禁忌
        List<String> restrictions = extractDietaryRestrictions(message);
        if (!restrictions.isEmpty()) {
            keyInfo.put("dietaryRestrictions", restrictions);
        }
        
        log.debug("提取关键信息：{} -> {}", message, keyInfo);
        return keyInfo;
    }
    
    /**
     * 进行情感分析
     */
    public SentimentType analyzeSentiment(String message) {
        String lowerMessage = message.toLowerCase();
        int positiveScore = 0;
        int negativeScore = 0;
        
        // 检查积极关键词
        for (String keyword : SENTIMENT_KEYWORDS.get("positive")) {
            if (lowerMessage.contains(keyword)) {
                positiveScore++;
            }
        }
        
        // 检查消极关键词
        for (String keyword : SENTIMENT_KEYWORDS.get("negative")) {
            if (lowerMessage.contains(keyword)) {
                negativeScore++;
            }
        }
        
        if (positiveScore > negativeScore) {
            return SentimentType.POSITIVE;
        } else if (negativeScore > positiveScore) {
            return SentimentType.NEGATIVE;
        } else {
            return SentimentType.NEUTRAL;
        }
    }
    
    /**
     * 消息路由决策
     */
    public String routeMessage(ChatRequest request, ConversationContext context) {
        MessageType messageType = analyzeMessageType(request.getMessage());
        ConversationContext.ConversationState currentState = context.getCurrentState();
        
        // 根据消息类型和当前状态决定路由策略
        switch (messageType) {
            case REQUIREMENT_DESCRIPTION:
                if (currentState == ConversationContext.ConversationState.INITIAL || 
                    currentState == ConversationContext.ConversationState.REQUIREMENT_GATHERING) {
                    return "AGENT_CHAIN"; // 走完整的Agent链路
                } else {
                    return "UPDATE_REQUIREMENT"; // 更新需求
                }
                
            case CONFIRMATION:
                if (currentState == ConversationContext.ConversationState.RECOMMENDATION_SHOWING) {
                    return "CONFIRM_RECOMMENDATION"; // 确认推荐
                } else {
                    return "AGENT_CHAIN";
                }
                
            case INQUIRY:
                return "DIRECT_RAG"; // 直接走RAG查询
                
            case FEEDBACK:
                return "PROCESS_FEEDBACK"; // 处理反馈
                
            case MODIFICATION:
                return "MODIFY_RECOMMENDATION"; // 修改推荐
                
            case GREETING:
                return "SIMPLE_RESPONSE"; // 简单回复
                
            default:
                return "AGENT_CHAIN"; // 默认走Agent链路
        }
    }
    
    /**
     * 后处理响应
     */
    public ChatResponse postProcessResponse(ChatResponse originalResponse, ConversationContext context) {
        if (originalResponse == null) {
            return createErrorResponse("系统暂时无法处理您的请求，请稍后再试。");
        }
        
        // 1. 添加个性化元素
        addPersonalization(originalResponse, context);
        
        // 2. 优化回复语气
        optimizeResponseTone(originalResponse, context);
        
        // 3. 添加引导性问题
        addGuidingQuestions(originalResponse, context);
        
        // 4. 格式化回复
        formatResponse(originalResponse);
        
        return originalResponse;
    }
    
    /**
     * 创建对话消息
     */
    public ConversationMessage createConversationMessage(String content, String type) {
        return ConversationMessage.builder()
                .messageType(type)
                .content(content)
                .build();
    }
    
    // ==================== 私有辅助方法 ====================
    
    private static void initIntentPatterns() {
        // 需求描述类
        INTENT_PATTERNS.put(".*(我们|我想|想要|需要).*(吃|用餐|点菜|订餐).*", MessageType.REQUIREMENT_DESCRIPTION);
        INTENT_PATTERNS.put(".*(推荐|介绍|建议).*(菜|美食|料理).*", MessageType.REQUIREMENT_DESCRIPTION);
        INTENT_PATTERNS.put(".*\\d+.*人.*(吃|用餐).*", MessageType.REQUIREMENT_DESCRIPTION);
        
        // 确认类
        INTENT_PATTERNS.put(".*(好的|可以|行|就这些|确定|下单).*", MessageType.CONFIRMATION);
        INTENT_PATTERNS.put(".*(要了|就要|选择).*(这|那).*", MessageType.CONFIRMATION);
        
        // 询问类
        INTENT_PATTERNS.put(".*(什么|怎么|如何).*", MessageType.INQUIRY);
        INTENT_PATTERNS.put(".*(详细|介绍|说说).*(这道|那道|这个|那个).*", MessageType.INQUIRY);
        
        // 反馈类
        INTENT_PATTERNS.put(".*(好吃|难吃|不错|一般|满意|不满意).*", MessageType.FEEDBACK);
        INTENT_PATTERNS.put(".*(喜欢|不喜欢|爱吃|不爱吃).*", MessageType.FEEDBACK);
        
        // 修改类
        INTENT_PATTERNS.put(".*(换个|换一个|不要|别的|其他).*", MessageType.MODIFICATION);
        INTENT_PATTERNS.put(".*(太贵|太便宜|太辣|不够辣).*", MessageType.MODIFICATION);
        
        // 问候类
        INTENT_PATTERNS.put(".*(你好|您好|hello|hi|早|晚上好).*", MessageType.GREETING);
    }
    
    private static void initSentimentKeywords() {
        SENTIMENT_KEYWORDS.put("positive", Arrays.asList(
            "好", "棒", "不错", "满意", "喜欢", "爱", "美味", "好吃", "赞", "推荐",
            "开心", "[开心]", "[满意]", "谢谢", "感谢"
        ));
        
        SENTIMENT_KEYWORDS.put("negative", Arrays.asList(
            "不好", "差", "不满意", "不喜欢", "难吃", "失望", "生气", "不推荐",
            "难过", "[难过]", "[不满意]", "投诉", "退款"
        ));
    }
    
    private Integer extractPeopleCount(String message) {
        Pattern pattern = Pattern.compile("(\\d+)\\s*[个]?人");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }
    
    private Integer extractBudget(String message) {
        Pattern pattern = Pattern.compile("(\\d+)\\s*[元块钱]");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }
    
    private List<String> extractTastePreferences(String message) {
        List<String> tastes = new ArrayList<>();
        String[] tasteKeywords = {"辣", "甜", "酸", "咸", "清淡", "重口", "麻", "香"};
        
        for (String taste : tasteKeywords) {
            if (message.contains(taste)) {
                tastes.add(taste);
            }
        }
        
        return tastes;
    }
    
    private String extractCuisineType(String message) {
        String[] cuisines = {"川菜", "粤菜", "湘菜", "鲁菜", "苏菜", "浙菜", "闽菜", "徽菜", "东北菜", "西餐"};
        
        for (String cuisine : cuisines) {
            if (message.contains(cuisine)) {
                return cuisine;
            }
        }
        
        return null;
    }
    
    private String extractDiningPurpose(String message) {
        if (message.contains("聚餐") || message.contains("聚会")) return "聚餐";
        if (message.contains("商务") || message.contains("工作")) return "商务";
        if (message.contains("约会") || message.contains("情侣")) return "约会";
        if (message.contains("家庭") || message.contains("家人")) return "家庭";
        if (message.contains("快餐") || message.contains("快速")) return "快餐";
        
        return null;
    }
    
    private List<String> extractDietaryRestrictions(String message) {
        List<String> restrictions = new ArrayList<>();
        String[] restrictionKeywords = {"素食", "不吃肉", "不吃辣", "不吃海鲜", "不吃牛肉", "不吃猪肉"};
        
        for (String restriction : restrictionKeywords) {
            if (message.contains(restriction)) {
                restrictions.add(restriction);
            }
        }
        
        return restrictions;
    }
    
    private void addPersonalization(ChatResponse response, ConversationContext context) {
        // 根据用户历史偏好添加个性化元素
        if (context.getUserPreference("preferredCuisine", null) != null) {
            // 可以在回复中提及用户偏好的菜系
        }
    }
    
    private void optimizeResponseTone(ChatResponse response, ConversationContext context) {
        // 根据对话轮次和用户情感调整语气
        if (context.getConversationRound() != null && context.getConversationRound() > 3) {
            // 多轮对话后使用更亲切的语气
        }
    }
    
    private void addGuidingQuestions(ChatResponse response, ConversationContext context) {
        // 根据当前状态添加引导性问题
        if (response.getFollowUpQuestions() == null || response.getFollowUpQuestions().isEmpty()) {
            List<String> questions = new ArrayList<>();
            
            if (context.getCurrentState() == ConversationContext.ConversationState.REQUIREMENT_GATHERING) {
                questions.add("还有其他特殊要求吗？");
                questions.add("对用餐环境有什么偏好吗？");
            }
            
            response.setFollowUpQuestions(questions);
        }
    }
    
    private void formatResponse(ChatResponse response) {
        // 格式化回复文本，确保良好的阅读体验
        if (response.getMessageText() != null) {
            String formatted = response.getMessageText()
                    .replaceAll("\\s+", " ")
                    .trim();
            response.setMessageText(formatted);
        }
    }
    
    private ChatResponse createErrorResponse(String message) {
        ChatResponse response = new ChatResponse();
        response.setMessageText(message);
        response.setFollowUpQuestions(Arrays.asList("您可以重新描述一下需求吗？", "需要我为您推荐一些热门菜品吗？"));
        return response;
    }
} 