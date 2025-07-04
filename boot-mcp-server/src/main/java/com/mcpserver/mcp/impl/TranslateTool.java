package com.mcpserver.mcp.impl;

import com.mcpserver.mcp.McpTool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.ai.tool.annotation.Tool;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@Component
public class TranslateTool implements McpTool {
    @Value("${mcp.translate.app-id}")
    private String appId;
    @Value("${mcp.translate.app-secret}")
    private String appSecret;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getName() { return "translate-mcp"; }

    @Override
    public String getDescription() { return "中英文互译工具（百度翻译API）"; }

    @Override
    @Tool(name = "translate-mcp", description = "中英文互译工具（百度翻译API）")
    public Object call(Map<String, Object> params) {
        String text = (String) params.get("text");
        if (text == null || text.trim().isEmpty()) {
            return "翻译失败: 待翻译文本不能为空";
        }
        String target = (String) params.getOrDefault("target", "en");
        if (target == null || target.trim().isEmpty()) {
            target = "en";
        }
        String from = target.equals("en") ? "zh" : "en";
        String to = target;
        String salt = String.valueOf(System.currentTimeMillis());
        String sign = md5(appId + text + salt + appSecret);
        try {
            String url = "https://fanyi-api.baidu.com/api/trans/vip/translate?q=" +
                URLEncoder.encode(text, StandardCharsets.UTF_8) +
                "&from=" + from + "&to=" + to + "&appid=" + appId + "&salt=" + salt + "&sign=" + sign;
            String json = restTemplate.getForObject(url, String.class);
            if (json != null && json.contains("error_code")) {
                return "翻译失败: " + json;
            }
            return json;
        } catch (Exception e) {
            return "翻译失败: " + e.getMessage();
        }
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
} 