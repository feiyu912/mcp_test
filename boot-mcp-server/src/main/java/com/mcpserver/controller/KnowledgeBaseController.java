package com.mcpserver.controller;

import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/knowledge-base")
public class KnowledgeBaseController {
    private final RedisVectorStore vectorStore;

    @Autowired
    public KnowledgeBaseController(RedisVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostMapping("/ask")
    public List<Document> askQuestion(@RequestBody String question) {
        SearchRequest request = SearchRequest.builder()
            .query(question)
            .topK(5)
            .build();
        return vectorStore.similaritySearch(request);
    }
} 