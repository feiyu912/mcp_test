package com.mcpserver.reader;

import com.mcpserver.model.Document;
import java.io.IOException;
import java.util.List;

public interface DocumentReader {
    List<Document> read() throws IOException;
} 