# MCP工具调用修复说明

## 问题描述
原项目中MCP工具调用存在以下问题：
1. MCP工具注册不完整，只有MysqlServiceImpl被注册
2. 前端传递的工具参数没有被正确使用
3. 工具调用逻辑有问题，没有正确集成用户输入
4. 工具描述不够清晰，AI难以正确调用

## 修复内容

### 1. 完善MCP工具注册
**文件**: `boot-mcp-server/src/main/java/com/mcpserver/ToolCallbackProviderConfig.java`

- 修改了`ToolCallbackProviderConfig`类，注册所有MCP工具实现
- 包括：AmapTool, WeatherTool, TranslateTool, TimeTool, RandomTool, FileSystemTool, UnitConvertTool, StockTool, MysqlServiceImpl
- **重要修复**: 重新设计了所有工具类，使其符合Spring AI的`@Tool`注解要求

### 2. 改进ChatController中的工具调用逻辑
**文件**: `boot-mcp-server/src/main/java/com/mcpserver/controller/ChatController.java`

- 添加了对前端传递的`tools`参数的处理
- 根据选择的工具动态生成工具描述
- 改进了提示词模板，使其更清晰易懂
- 添加了调试日志，便于排查问题

### 3. 重新设计MCP工具实现
**文件**: `boot-mcp-server/src/main/java/com/mcpserver/mcp/impl/*.java`

**重要变更**: 所有工具类都重新设计为符合Spring AI框架要求的结构

#### 工具类结构变更
- 移除了`McpTool`接口，不再实现统一接口
- 每个工具类都是独立的Spring组件
- 使用`@Tool`注解标记具体的工具方法
- 方法参数直接使用具体类型，而不是Map

#### WeatherTool.java
- 方法名: `getWeather(String city)`
- 添加了城市名称清理和映射
- 支持常见城市的中英文名称
- 改进了错误处理和返回信息

#### TranslateTool.java
- 方法名: `translate(String text, String target)`
- 添加了智能语言检测
- 支持多种语言标识符（zh/chinese/中文等）
- 改进了参数处理和错误提示

#### AmapTool.java
- 方法名: `geocode(String address)`
- 添加了地址名称清理
- 改进了API返回状态解析
- 提供了更友好的错误信息

#### 其他工具
- TimeTool: `getCurrentTime()`
- RandomTool: `generateRandom(int min, int max)`
- FileSystemTool: `listFiles(String path)`
- UnitConvertTool: `convert(double value, String type)`
- StockTool: `getStockPrice(String code)`
- 统一改进了工具描述，使其更清晰
- 添加了参数说明和示例

### 4. 创建测试页面
**文件**: 
- `ai-front-end/src/pages/TestMcpPage.vue`
- `boot-mcp-server/src/main/java/com/mcpserver/controller/McpTestController.java`

- 创建了独立的MCP工具测试页面
- 提供了每个工具的独立测试接口
- 便于验证工具是否正常工作

### 5. 前端改进
**文件**: `ai-front-end/src/pages/ChatPage.vue`

- 添加了调试日志，显示发送的工具选择
- 改进了工具选择的传递逻辑

## 使用方法

### 1. 启动项目
```bash
# 启动后端
cd boot-mcp-server
mvn spring-boot:run

# 启动前端
cd ai-front-end
npm run dev
```

### 2. 测试MCP工具
1. 登录系统
2. 点击左侧菜单的"MCP测试"
3. 在测试页面中测试各种工具功能

### 3. 在对话中使用MCP工具
1. 进入对话页面
2. 在工具选择下拉框中选择需要的工具
3. 输入相关问题，AI会自动调用相应的工具

## 工具列表

| 工具名称 | 功能描述 | 参数 |
|---------|---------|------|
| weather-mcp | 天气查询 | city: 城市名称 |
| translate-mcp | 中英文翻译 | text: 待翻译文本, target: 目标语言 |
| time-mcp | 获取当前时间 | 无需参数 |
| random-mcp | 生成随机数 | min: 最小值, max: 最大值 |
| amap-mcp | 高德地图地理编码 | address: 地址名称 |
| unitconvert-mcp | 单位换算 | value: 数值, type: 换算类型 |
| stock-mcp | 股票查询 | code: 股票代码 |
| filesystem-mcp | 文件系统操作 | path: 文件路径 |

## 注意事项

1. 确保配置文件中的API密钥正确设置
2. 天气查询需要和风天气API密钥
3. 翻译功能需要百度翻译API密钥
4. 高德地图需要高德API密钥
5. 某些工具可能需要网络连接

## 调试

如果遇到问题，可以查看后端控制台的调试日志：
- `[DEBUG] 收到用户问题: xxx`
- `[DEBUG] 选择的工具: xxx`
- `[DEBUG] 流式输出: xxx`
- `[DEBUG] 最终AI回复: xxx`

前端控制台也会显示发送的工具选择信息。 