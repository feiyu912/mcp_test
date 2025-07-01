package com.mcpserver.transformer;

import com.mcpserver.model.Document;
import java.util.List;

public interface DocumentTransformer {
    List<Document> transform(List<Document> documents);
} 