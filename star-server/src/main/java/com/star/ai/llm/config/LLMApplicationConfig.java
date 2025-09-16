package com.star.ai.llm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.vectorstore.VectorStore;
// OpenAI
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingClient;
// DeepSeek（假设有Spring AI适配器，实际需引入官方或社区SDK）
// import org.springframework.ai.deepseek.DeepSeekChatClient;
// import org.springframework.ai.deepseek.DeepSeekEmbeddingClient;
// Qwen（通义千问，假设有Spring AI适配器，实际需引入官方或社区SDK）
// import org.springframework.ai.qwen.QwenChatClient;
// import org.springframework.ai.qwen.QwenEmbeddingClient;
// Milvus
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;

/**
 * LLM应用核心配置类
 * 负责Spring AI相关核心Bean的配置，包括ChatClient、EmbeddingClient、VectorStore等。
 *
 * Embedding服务说明：
 * Embedding服务用于将文本、图片等内容转化为高维向量，便于相似度检索、RAG等AI场景。
 * 推荐开源Embedding服务：
 * 1. BGE（BAAI General Embedding，https://github.com/FlagOpen/FlagEmbedding）
 * 2. sentence-transformers（https://www.sbert.net/，Python生态）
 * 3. HuggingFace上的MiniLM、E5、GTE等模型
 * 4. Java侧可通过HTTP调用本地/远程embedding服务，或用DJL加载ONNX模型
 */
@Configuration
public class LLMApplicationConfig {

    /**
     * 配置OpenAI ChatClient
     */
    @Bean
    @ConditionalOnProperty(name = "llm.provider", havingValue = "openai", matchIfMissing = true)
    public ChatClient openAiChatClient(
            @Value("${spring.ai.openai.api-key:}") String openAiApiKey,
            @Value("${spring.ai.openai.base-url:https://api.openai.com/v1}") String openAiBaseUrl) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel("gpt-3.5-turbo")
                .build();
        return new OpenAiChatClient(openAiApiKey, openAiBaseUrl, options);
    }

    /**
     * 配置DeepSeek ChatClient（如有官方/社区Spring AI适配器，可替换为真实实现）
     */
    // @Bean
    // @ConditionalOnProperty(name = "llm.provider", havingValue = "deepseek")
    // public ChatClient deepSeekChatClient(@Value("${spring.ai.deepseek.api-key:}") String deepSeekApiKey) {
    //     return new DeepSeekChatClient(deepSeekApiKey, ...);
    // }

    /**
     * 配置Qwen（通义千问）ChatClient（如有官方/社区Spring AI适配器，可替换为真实实现）
     */
    // @Bean
    // @ConditionalOnProperty(name = "llm.provider", havingValue = "qwen")
    // public ChatClient qwenChatClient(@Value("${spring.ai.qwen.api-key:}") String qwenApiKey) {
    //     return new QwenChatClient(qwenApiKey, ...);
    // }

    /**
     * 配置OpenAI EmbeddingClient
     */
    @Bean
    @ConditionalOnProperty(name = "llm.provider", havingValue = "openai", matchIfMissing = true)
    public EmbeddingClient openAiEmbeddingClient(
            @Value("${spring.ai.openai.api-key:}") String openAiApiKey,
            @Value("${spring.ai.openai.base-url:https://api.openai.com/v1}") String openAiBaseUrl) {
        return new OpenAiEmbeddingClient(openAiApiKey, openAiBaseUrl);
    }

    /**
     * 配置DeepSeek EmbeddingClient（如有官方/社区Spring AI适配器，可替换为真实实现）
     */
    // @Bean
    // @ConditionalOnProperty(name = "llm.provider", havingValue = "deepseek")
    // public EmbeddingClient deepSeekEmbeddingClient(@Value("${spring.ai.deepseek.api-key:}") String deepSeekApiKey) {
    //     return new DeepSeekEmbeddingClient(deepSeekApiKey, ...);
    // }

    /**
     * 配置Qwen（通义千问）EmbeddingClient（如有官方/社区Spring AI适配器，可替换为真实实现）
     */
    // @Bean
    // @ConditionalOnProperty(name = "llm.provider", havingValue = "qwen")
    // public EmbeddingClient qwenEmbeddingClient(@Value("${spring.ai.qwen.api-key:}") String qwenApiKey) {
    //     return new QwenEmbeddingClient(qwenApiKey, ...);
    // }

    /**
     * 配置Milvus VectorStore
     * 推荐使用Milvus 2.x，需先启动Milvus服务并配置好连接参数
     */
    @Bean
    public VectorStore vectorStore(
            @Value("${milvus.host:localhost}") String milvusHost,
            @Value("${milvus.port:19530}") int milvusPort) {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(milvusHost)
                .withPort(milvusPort)
                .build();
        MilvusServiceClient milvusClient = new MilvusServiceClient(connectParam);
        // "star_food_ai_vector"为集合名，可根据实际业务自定义
        return new MilvusVectorStore(milvusClient, "star_food_ai_vector");
    }
} 