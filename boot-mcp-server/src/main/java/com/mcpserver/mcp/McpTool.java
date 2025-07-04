package com.mcpserver.mcp;

import java.util.Map;

public interface McpTool {
    String getName();
    String getDescription();
    Object call(Map<String, Object> params);
} 