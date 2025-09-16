package com.star.ai.rag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.document.Document;
import java.util.List;

/**
 * 向量检索服务
 * 负责基于向量的相似度检索。
 */
@Service
public class VectorSearchService {

    @Autowired
    private VectorStore vectorStore;

    /**
     * 默认参数的相似度检索
     * @param query 查询内容
     * @return 相似文档列表
     */
    public List<Document> similaritySearch(String query) {
        return similaritySearch(query, 5, 0.7, null);
    }

    /**
     * 支持多知识库、动态切换、检索参数定制
     * @param query 查询内容
     * @param topK 返回文档数
     * @param threshold 相似度阈值
     * @param vectorStoreName 可选知识库名
     * @return 相似文档列表
     */
    public List<Document> similaritySearch(String query, int topK, double threshold, String vectorStoreName) {
        // TODO: 根据vectorStoreName动态切换知识库（如有多知识库实现）
        SearchRequest request = SearchRequest.query(query)
                .withTopK(topK)
                .withSimilarityThreshold(threshold);
        return vectorStore.similaritySearch(request);
    }
} 