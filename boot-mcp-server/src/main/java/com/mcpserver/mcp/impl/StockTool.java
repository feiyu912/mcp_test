package com.mcpserver.mcp.impl;

import com.mcpserver.mcp.McpTool;
import org.springframework.stereotype.Component;
import org.springframework.ai.tool.annotation.Tool;
import java.util.Map;

@Component
public class StockTool implements McpTool {
    @Override
    public String getName() { return "stock-mcp"; }

    @Override
    public String getDescription() { return "股票查询工具（模拟）"; }

    @Override
    @Tool(name = "stock-mcp", description = "股票查询工具（模拟）")
    public Object call(Map<String, Object> params) {
        String code = (String) params.get("code");
        if (code == null || code.trim().isEmpty()) {
            return "股票查询失败: 股票代码不能为空";
        }
        // 这里只做模拟，实际可接第三方API
        return "股票 " + code + " 当前价格：" + (100 + Math.random() * 10);
    }
} 