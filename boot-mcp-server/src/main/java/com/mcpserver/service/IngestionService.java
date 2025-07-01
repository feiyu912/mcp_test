package com.mcpserver.service;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class IngestionService {
    private final VectorStore vectorStore;

    @Autowired
    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void ingestDataFromString(String content) {
        // 先按段落分割（遇到空行或换行符分段）
        String[] paragraphs = content.split("\\r?\\n+|\\n{2,}");
        List<Document> docs = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int maxLen = 1000; // 每段最大字数
        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (!trimmed.isEmpty()) {
                if (sb.length() + trimmed.length() > maxLen) {
                    docs.add(new Document(sb.toString()));
                    sb.setLength(0);
                }
                if (sb.length() > 0) sb.append('\n');
                sb.append(trimmed);
            }
        }
        if (sb.length() > 0) {
            docs.add(new Document(sb.toString()));
        }
        // 分批入库，每批最多25条
        int batchSize = 25;
        for (int i = 0; i < docs.size(); i += batchSize) {
            int end = Math.min(i + batchSize, docs.size());
            List<Document> batch = docs.subList(i, end);
            vectorStore.add(batch);
        }
    }
} 