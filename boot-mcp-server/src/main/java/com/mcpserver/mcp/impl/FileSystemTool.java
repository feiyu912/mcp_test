package com.mcpserver.mcp.impl;

import com.mcpserver.mcp.McpTool;
import org.springframework.stereotype.Component;
import org.springframework.ai.tool.annotation.Tool;
import java.io.File;
import java.util.Map;

@Component
public class FileSystemTool implements McpTool {
    @Override
    public String getName() { return "filesystem-mcp"; }

    @Override
    public String getDescription() { return "文件系统操作工具"; }

    @Override
    @Tool(name = "filesystem-mcp", description = "文件系统操作工具")
    public Object call(Map<String, Object> params) {
        String path = (String) params.get("path");
        if (path == null || path.trim().isEmpty()) {
            return "文件系统操作失败: 路径不能为空";
        }
        File file = new File(path);
        if (!file.exists()) {
            return "文件系统操作失败: 路径不存在";
        }
        if (file.isDirectory()) {
            String[] files = file.list();
            return "目录内容：" + String.join(", ", files != null ? files : new String[0]);
        } else {
            return "文件：" + file.getName();
        }
    }
} 