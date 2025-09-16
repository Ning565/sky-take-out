package com.star.ai.rag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.document.Document;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG核心服务
 * 负责检索增强生成（Retrieval-Augmented Generation）。
 */
@Service
public class RAGService {

    @Autowired
    private ChatClient chatClient;  // Spring AI ChatClient

    @Autowired
    private VectorStore vectorStore;  // Spring AI VectorStore

    /**
     * RAG核心：检索增强生成
     * @param userQuery 用户问题
     * @param context   对话上下文（可为自定义对象）
     * @return 智能回复
     */
    public String ragGenerate(String userQuery, Object context) {
        // 1. 向量化查询并检索相关文档（使用Spring AI SearchRequest和Document）
        List<Document> relevantDocs = vectorStore.similaritySearch(
                SearchRequest.query(userQuery)
                        .withTopK(5)
                        .withSimilarityThreshold(0.7)
        );

        // 2. 构建增强上下文
        String enhancedContext = buildEnhancedContext(relevantDocs, context);

        // 3. 构建RAG Prompt
        String ragPrompt = buildRAGPrompt(userQuery, enhancedContext);

        // 4. 大模型生成回复
        return chatClient.call(ragPrompt);
    }

    /**
     * 对外暴露：知识库检索接口，返回结构化文档内容
     * @param query 检索内容
     * @param topK 返回文档数
     * @param threshold 相似度阈值
     * @return 文档内容列表
     */
    public List<String> searchKnowledge(String query, int topK, double threshold) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.query(query)
                        .withTopK(topK)
                        .withSimilarityThreshold(threshold)
        );
        return docs.stream().map(Document::getContent).collect(Collectors.toList());
    }

    /**
     * 拼接知识库内容和对话上下文，生成增强上下文
     * @param docs    检索到的相关知识文档
     * @param context 对话上下文（可为自定义对象）
     * @return 增强上下文字符串
     */
    private String buildEnhancedContext(List<Document> docs, Object context) {
        StringBuilder sb = new StringBuilder();
        sb.append("【知识库】\n");
        for (Document doc : docs) {
            sb.append(doc.getContent()).append("\n");
        }
        // 如有自定义上下文信息，可追加
        if (context != null) {
            sb.append("【对话上下文】\n");
            sb.append(context.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 构建RAG Prompt，要求大模型基于知识库内容进行智能推荐和回复
     * @param userQuery 用户问题
     * @param enhancedContext 增强上下文
     * @return RAG Prompt字符串
     */
    private String buildRAGPrompt(String userQuery, String enhancedContext) {
        return String.format(
                "你是一个专业的餐饮推荐助手。请基于以下知识库信息回答用户问题。\n\n%s\n用户问题：%s\n\n要求：\n1. 必须基于知识库中的真实菜品信息进行推荐\n2. 不要编造不存在的菜品或价格\n3. 根据用户偏好进行个性化推荐\n4. 回复要自然友好，包含推荐理由\n5. 如果知识库中没有合适信息，诚实说明并引导用户\n\n回复：",
                enhancedContext, userQuery
        );
    }
} 