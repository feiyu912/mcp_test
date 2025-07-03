package com.mcpserver.service;

import org.apache.tika.Tika;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Service
public class ChatSessionFileService {
    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private VectorStore vectorStore;

    /**
     * 解析文件内容，embedding后存入vectorStore
     * @param sessionId 会话ID
     * @param file 上传的文件
     */
    public void saveSessionFileEmbedding(Long sessionId, MultipartFile file) throws Exception {
        // 1. 解析文件内容
        Tika tika = new Tika();
        String content = tika.parseToString(file.getInputStream());
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("文件内容为空");
        }
        // 2. 按1000字分段
        int maxLen = 1000;
        List<String> segments = new ArrayList<>();
        for (int i = 0; i < content.length(); i += maxLen) {
            int end = Math.min(i + maxLen, content.length());
            segments.add(content.substring(i, end));
        }
        // 3. 每段分别embedding并存入vectorStore
        int segNo = 1;
        for (String seg : segments) {
            if (seg.trim().isEmpty()) continue;
            embeddingModel.embed(List.of(seg)); // 仅用于生成embedding，实际embedding由vectorStore管理
            Map<String, Object> meta = new HashMap<>();
            meta.put("type", "session");
            meta.put("sessionId", sessionId.toString());
            meta.put("fileName", file.getOriginalFilename());
            meta.put("segmentNo", segNo);
            Document doc = new Document(seg, meta);
            vectorStore.add(List.of(doc));
            segNo++;
        }
    }
} 