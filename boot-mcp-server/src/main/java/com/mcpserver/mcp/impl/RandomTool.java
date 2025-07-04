package com.mcpserver.mcp.impl;

import com.mcpserver.mcp.McpTool;
import org.springframework.stereotype.Component;
import org.springframework.ai.tool.annotation.Tool;
import java.util.Map;
import java.util.Random;

@Component
public class RandomTool implements McpTool {
    @Override
    public String getName() { return "random-mcp"; }

    @Override
    public String getDescription() { return "生成随机整数"; }

    @Override
    @Tool(name = "random-mcp", description = "生成随机整数")
    public Object call(Map<String, Object> params) {
        try {
            Object minObj = params.getOrDefault("min", 0);
            Object maxObj = params.getOrDefault("max", 100);
            int min, max;
            try {
                min = Integer.parseInt(minObj.toString());
                max = Integer.parseInt(maxObj.toString());
            } catch (Exception e) {
                return "随机数生成失败: min/max 参数必须为整数";
            }
            if (min > max) {
                return "随机数生成失败: min 不能大于 max";
            }
            return "随机数：" + (new Random().nextInt(max - min + 1) + min);
        } catch (Exception e) {
            return "随机数生成失败: " + e.getMessage();
        }
    }
} 