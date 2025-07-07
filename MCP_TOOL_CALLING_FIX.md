# Spring AI MCP 工具调用修复方案

## 问题分析

根据你的描述，Spring AI MCP 工具调用存在以下问题：
1. **MethodToolCallbackProvider 报错**："No @Tool annotated methods found..."
2. **工具调用无法正常工作**：AI 回复中没有"【工具】xxx-mcp:"块
3. **数据库 mcpContent 字段为空**：工具调用结果未保存
4. **历史会话正常，新会话异常**：工具注册链路有问题

## 根本原因

1. **依赖冲突**：pom.xml 中有重复的 spring-ai 依赖
2. **工具注册方式错误**：使用了不兼容的注册方式
3. **ChatController 工具调用逻辑有问题**：没有正确注册工具到 ChatClient

## 修复方案

### 1. 清理依赖冲突

**文件**: `boot-mcp-server/pom.xml`

- 统一 spring-ai 版本为 1.0.0
- 移除重复的 `spring-ai-advisors-vector-store` 依赖
- 添加版本变量管理

```xml
<properties>
    <spring-ai.version>1.0.0</spring-ai.version>
</properties>
```

### 2. 重构工具配置类

**文件**: `boot-mcp-server/src/main/java/com/mcpserver/ToolCallbackProviderConfig.java`

- 使用 `MethodToolCallbackProvider` 方式注册工具
- 确保所有工具类都被正确注册

```java
@Bean
public MethodToolCallbackProvider mcpToolCallbacks(
        AmapTool amapTool,
        WeatherTool weatherTool,
        // ... 其他工具
) {
    return MethodToolCallbackProvider.builder()
            .toolObjects(
                    amapTool,
                    weatherTool,
                    // ... 其他工具
            )
            .build();
}
```

### 3. 修复 ChatController 工具调用逻辑

**文件**: `boot-mcp-server/src/main/java/com/mcpserver/controller/ChatController.java`

- 注入 `MethodToolCallbackProvider` Bean
- 使用 `.toolCallbacks()` 方法注册工具
- 改进提示词模板，更清晰地说明工具调用

```java
@Autowired
private MethodToolCallbackProvider mcpToolCallbacks;

// 在对话接口中使用
return chatClient.prompt()
        .user(safePrompt)
        .advisors(qaAdvisor)
        .toolCallbacks(mcpToolCallbacks) // 使用注入的MCP工具
        .stream()
        .content()
```

### 4. 确保工具类正确注解

所有工具类都必须：
- 使用 `@Component` 注解注册为 Spring Bean
- 使用 `@Tool` 注解标记工具方法
- 提供清晰的工具描述

```java
@Component
public class TimeTool {
    @Tool(name = "time-mcp", description = "获取当前时间工具，无需参数")
    public String getCurrentTime() {
        // 实现逻辑
    }
}
```

## 测试验证

### 1. 启动测试

运行 `start-test.bat` 脚本：
```bash
start-test.bat
```

### 2. 功能测试

1. **访问前端**: http://localhost:5173
2. **注册/登录**: 创建账号并登录
3. **MCP测试页面**: 点击左侧菜单"MCP测试"
4. **基础工具测试**:
   - 测试时间工具
   - 测试随机数工具
   - 测试天气工具
5. **AI对话测试**:
   - 输入"现在几点了？"
   - 验证是否调用了 time-mcp 工具

### 3. 调试信息

查看后端控制台日志：
```
[DEBUG] 收到用户问题: 现在几点了？
[DEBUG] AI原始回复: 当前时间：2024-01-01 12:00:00
[DEBUG] 提取到的MCP内容: 【工具】time-mcp: 当前时间：2024-01-01 12:00:00
```

## 工具列表

| 工具名称 | 功能描述 | 参数 |
|---------|---------|------|
| time-mcp | 获取当前时间 | 无需参数 |
| weather-mcp | 天气查询 | city: 城市名称 |
| translate-mcp | 中英文翻译 | text: 待翻译文本, target: 目标语言 |
| amap-mcp | 高德地图地理编码 | address: 地址名称 |
| random-mcp | 生成随机数 | min: 最小值, max: 最大值 |
| filesystem-mcp | 文件系统操作 | path: 文件路径 |
| unitconvert-mcp | 单位换算 | value: 数值, type: 换算类型 |
| stock-mcp | 股票查询 | code: 股票代码 |

## 常见问题排查

### 1. 工具无法被识别

**检查项**:
- 工具类是否有 `@Component` 注解
- 工具方法是否有 `@Tool` 注解
- 是否有依赖冲突（检查 pom.xml）

**解决方案**:
```bash
# 清理并重新编译
mvn clean compile
```

### 2. 工具调用结果未返回

**检查项**:
- ChatController 是否正确注入了 `MethodToolCallbackProvider`
- 是否使用了 `.toolCallbacks()` 方法
- 提示词模板是否清晰

**解决方案**:
```java
// 确保正确注入
@Autowired
private MethodToolCallbackProvider mcpToolCallbacks;

// 确保正确使用
.toolCallbacks(mcpToolCallbacks)
```

### 3. 数据库 mcpContent 为空

**检查项**:
- 正则表达式是否正确提取工具调用内容
- 工具调用结果是否包含"【工具】"标记

**解决方案**:
```java
// 改进正则表达式
String regex = "【工具】[\\w\\-]+-mcp:[\\s\\S]*?(?=(【工具】[\\w\\-]+-mcp:|$))";
```

## 性能优化建议

1. **工具缓存**: 对于频繁调用的工具，考虑添加缓存
2. **异步处理**: 对于耗时工具，考虑异步处理
3. **错误处理**: 添加完善的错误处理和重试机制
4. **监控日志**: 添加详细的监控和日志记录

## 总结

通过以上修复方案，Spring AI MCP 工具调用应该能够正常工作。关键点是：

1. **依赖管理**: 确保 spring-ai 依赖版本一致，无冲突
2. **工具注册**: 使用正确的注册方式，确保所有工具都被识别
3. **调用链路**: 在 ChatController 中正确使用工具
4. **测试验证**: 通过测试页面验证功能是否正常

如果仍有问题，请检查后端控制台日志，定位具体的错误信息。 