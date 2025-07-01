package com.mcpserver.transformer;

import com.mcpserver.model.Document;
import java.util.ArrayList;
import java.util.List;

public class TokenTextSplitter implements DocumentTransformer {
    @Override
    public List<Document> transform(List<Document> documents) {
        List<Document> transformedDocuments = new ArrayList<>();
        for (Document doc : documents) {
            String content = doc.getContent();
            String[] tokens = content.split("\\s+");
            for (String token : tokens) {
                if (!token.isEmpty()) {
                    transformedDocuments.add(new Document(token));
                }
            }
        }
        return transformedDocuments;
    }
} 