package com.mcpserver;

import com.mcpserver.mcp.impl.MysqlServiceImpl;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TODO
 *
 * @Author: ws
 * @Date: 2025/4/27 15:41
 */
@Configuration
public class ToolCallbackProviderConfig {

    @Bean
    public ToolCallbackProvider MysqlTools(MysqlServiceImpl mysqlService) {
        return MethodToolCallbackProvider.builder().toolObjects(mysqlService).build();
    }
}