package com.mcpserver.mcp.impl;

import com.mcpserver.mcp.McpTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.ai.tool.annotation.Tool;
import java.util.Map;

@Component
public class WeatherTool implements McpTool {
    @Value("${mcp.weather.api-key}")
    private String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getName() { return "weather-mcp"; }

    @Override
    public String getDescription() { return "天气查询工具（和风天气API）"; }

    @Override
    @Tool(name = "weather-mcp", description = "天气查询工具（和风天气API）")
    public Object call(Map<String, Object> params) {
        String city = (String) params.get("city");
        if (city == null || city.trim().isEmpty()) {
            return "天气查询失败: 城市不能为空";
        }
        String url = "https://devapi.qweather.com/v7/weather/now?location=" + city + "&key=" + apiKey;
        try {
            String json = restTemplate.getForObject(url, String.class);
            if (json != null && json.contains("code")) {
                // 解析和风天气API的错误码
                if (json.contains("403")) {
                    return "天气查询失败: 403 权限不足或API密钥无效";
                } else if (json.contains("401")) {
                    return "天气查询失败: 401 未授权，API密钥错误";
                }
            }
            return json;
        } catch (Exception e) {
            return "天气查询失败: " + e.getMessage();
        }
    }
} 