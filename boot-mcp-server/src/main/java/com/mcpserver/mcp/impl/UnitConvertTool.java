package com.mcpserver.mcp.impl;

import com.mcpserver.mcp.McpTool;
import org.springframework.stereotype.Component;
import org.springframework.ai.tool.annotation.Tool;
import java.util.Map;

@Component
public class UnitConvertTool implements McpTool {
    @Override
    public String getName() { return "unitconvert-mcp"; }

    @Override
    public String getDescription() { return "单位换算工具"; }

    @Override
    @Tool(name = "unitconvert-mcp", description = "单位换算工具")
    public Object call(Map<String, Object> params) {
        double value;
        String type = (String) params.getOrDefault("type", "m2ft");
        try {
            value = Double.parseDouble(params.getOrDefault("value", 0).toString());
        } catch (Exception e) {
            return "单位换算失败: value 参数必须为数字";
        }
        switch (type) {
            case "m2ft":
                return value + "米 = " + (value * 3.28084) + "英尺";
            case "ft2m":
                return value + "英尺 = " + (value / 3.28084) + "米";
            default:
                return "单位换算失败: 不支持的类型";
        }
    }
} 