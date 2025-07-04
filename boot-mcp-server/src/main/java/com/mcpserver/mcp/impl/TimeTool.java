package com.mcpserver.mcp.impl;

import com.mcpserver.mcp.McpTool;
import org.springframework.stereotype.Component;
import org.springframework.ai.tool.annotation.Tool;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class TimeTool implements McpTool {
    @Override
    public String getName() { return "time-mcp"; }

    @Override
    public String getDescription() { return "获取当前时间"; }

    @Override
    @Tool(name = "time-mcp", description = "获取当前时间")
    public Object call(Map<String, Object> params) {
        LocalDateTime now = LocalDateTime.now();
        return "当前时间：" + now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
} 