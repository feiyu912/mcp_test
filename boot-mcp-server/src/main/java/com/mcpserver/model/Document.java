package com.mcpserver.model;

import java.util.List;
import java.util.UUID;

public class Document {
    private String id;
    private String content;
    private List<Float> embedding;

    public Document(String content) {
        this.id = UUID.randomUUID().toString();
        this.content = content;
    }

    public Document(String id, String content, List<Float> embedding) {
        this.id = id;
        this.content = content;
        this.embedding = embedding;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<Float> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Float> embedding) {
        this.embedding = embedding;
    }
} 