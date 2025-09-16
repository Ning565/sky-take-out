package com.star.ai.rag;

import org.springframework.stereotype.Component;
import org.springframework.ai.document.Document;
import java.util.List;

/**
 * 文档处理器
 * 负责知识文档的处理与上下文增强。
 */
@Component
public class DocumentProcessor {
    /**
     * 处理知识文档，生成增强上下文
     * @param docs    检索到的相关知识文档
     * @param context 对话上下文（可为自定义对象）
     * @return 增强上下文字符串
     */
    public String processDocuments(List<Document> docs, Object context) {
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
} 