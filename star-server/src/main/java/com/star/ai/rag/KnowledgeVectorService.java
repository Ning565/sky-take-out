package com.star.ai.rag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.document.Document;
import java.util.List;
import java.util.ArrayList;

// 假设有DishMapper和Dish实体
import com.star.pojo.entity.Dish;
import com.star.server.mapper.DishMapper;

/**
 * 知识向量化服务
 * 负责知识库的向量化与存储。
 */
@Service
public class KnowledgeVectorService {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private EmbeddingClient embeddingClient;

    @Autowired
    private DishMapper dishMapper;

    /**
     * 构建菜品知识向量库
     * 实际获取菜品数据、构建Document、向量化并入库
     */
    public void buildDishVectorDatabase() {
        // 1. 获取所有菜品数据
        List<Dish> allDishes = dishMapper.selectAll();
        List<Document> documents = new ArrayList<>();
        for (Dish dish : allDishes) {
            // 2. 构建富文本描述
            StringBuilder desc = new StringBuilder();
            desc.append("菜品名称：").append(dish.getName()).append("\n");
            desc.append("价格：").append(dish.getPrice()).append("元\n");
            desc.append("描述：").append(dish.getDescription()).append("\n");
            // 可扩展更多标签、口味、菜系等信息
            // 3. 创建Document对象
            Document doc = new Document(desc.toString());
            doc.getMetadata().put("dishId", dish.getId());
            doc.getMetadata().put("dishName", dish.getName());
            doc.getMetadata().put("price", dish.getPrice());
            doc.getMetadata().put("categoryId", dish.getCategoryId());
            documents.add(doc);
        }
        // 4. 批量向量化并存储
        vectorStore.add(documents);
    }
} 