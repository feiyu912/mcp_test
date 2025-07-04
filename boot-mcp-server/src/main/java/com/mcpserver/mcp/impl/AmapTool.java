package com.mcpserver.mcp.impl;

import com.mcpserver.mcp.McpTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.ai.tool.annotation.Tool;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class AmapTool implements McpTool {
    @Value("${mcp.amap.api-key}")
    private String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getName() { return "amap-mcp"; }

    @Override
    public String getDescription() { return "高德地图地理编码（真实API）"; }

    @Override
    @Tool(name = "amap-mcp", description = "高德地图地理编码（真实API）")
    public Object call(Map<String, Object> params) {
        String address = (String) params.get("address");
        if (address == null || address.trim().isEmpty()) {
            return "高德地理编码失败: 地址不能为空";
        }
        try {
            String url = "https://restapi.amap.com/v3/geocode/geo?address=" +
                    URLEncoder.encode(address, StandardCharsets.UTF_8) + "&key=" + apiKey;
            String json = restTemplate.getForObject(url, String.class);
            return json;
        } catch (Exception e) {
            return "高德地理编码失败: " + e.getMessage();
        }
    }
}
