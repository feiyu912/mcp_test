package com.mcpserver.reader;

import com.mcpserver.model.Document;
import org.springframework.core.io.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TextReader implements DocumentReader {
    private final Resource resource;

    public TextReader(Resource resource) {
        this.resource = resource;
    }

    @Override
    public List<Document> read() throws IOException {
        List<Document> documents = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String line;
            StringBuilder content = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            documents.add(new Document(content.toString()));
        }
        return documents;
    }
} 