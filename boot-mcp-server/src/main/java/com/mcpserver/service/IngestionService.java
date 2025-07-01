package com.mcpserver.service;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
        Document doc = new Document(content);
        vectorStore.add(Collections.singletonList(doc));
    }
} 