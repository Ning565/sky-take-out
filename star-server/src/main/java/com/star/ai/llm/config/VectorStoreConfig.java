package com.star.ai.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * 向量存储配置类
 * 负责Milvus向量数据库的参数配置和初始化。
 * 支持通过配置文件灵活配置Milvus主机、端口、用户名、密码等参数。
 */
@Configuration
@ConfigurationProperties(prefix = "milvus")
@Data
public class VectorStoreConfig {
    /** Milvus主机地址 */
    private String host = "localhost";
    /** Milvus端口 */
    private int port = 19530;
    /** Milvus用户名（如有） */
    private String username;
    /** Milvus密码（如有） */
    private String password;
    /** Milvus数据库名（如有） */
    private String database;
    /** Milvus集合名（Collection） */
    private String collection = "star_food_ai_vector";
    // 可根据实际业务扩展更多参数
} 