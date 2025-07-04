package com.mcpserver.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/mcp")
public class McpController {

    // MCP 工具列表接口，静态返回所有工具信息
    @GetMapping("/tools")
    public List<Map<String, Object>> getTools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(Map.of(
            "name", "amap-mcp",
            "params", "address: string",
            "description", "高德地图地理编码"
        ));
        tools.add(Map.of(
            "name", "weather-mcp",
            "params", "city: string",
            "description", "天气查询"
        ));
        tools.add(Map.of(
            "name", "translate-mcp",
            "params", "text: string, target: string",
            "description", "翻译"
        ));
        tools.add(Map.of(
            "name", "time-mcp",
            "params", "",
            "description", "获取当前时间"
        ));
        tools.add(Map.of(
            "name", "random-mcp",
            "params", "min: int, max: int",
            "description", "生成随机整数"
        ));
        tools.add(Map.of(
            "name", "filesystem-mcp",
            "params", "path: string",
            "description", "文件系统操作"
        ));
        tools.add(Map.of(
            "name", "unitconvert-mcp",
            "params", "value: double, type: string",
            "description", "单位换算"
        ));
        tools.add(Map.of(
            "name", "stock-mcp",
            "params", "code: string",
            "description", "股票查询"
        ));
        return tools;
    }
} 