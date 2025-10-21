# AI智能问答平台

## 项目简介

AI智能问答平台是一个集成了知识库管理、AI对话和文档处理功能的全栈应用系统。系统采用前后端分离架构，前端提供用户友好的界面，后端负责处理AI模型交互、向量存储和文档处理等核心功能。

### 主要功能

- **AI对话系统**：支持多轮对话、历史会话管理、会话标题编辑和删除
- **知识库管理**：提供基于向量检索的知识库搜索功能
- **文件上传与处理**：支持上传文档并将内容导入知识库
- **用户认证**：包含用户注册、登录和会话管理功能

## 技术栈

### 前端

- **框架**：Vue 3 + Vite
- **UI组件库**：Element Plus
- **路由**：Vue Router 4
- **开发工具**：VSCode + Volar

### 后端

- **框架**：Spring Boot 3.4.3
- **AI集成**：Spring AI
- **向量存储**：Redis
- **数据库**：MySQL
- **文档处理**：Apache Tika
- **ORM**：MyBatis

## 项目结构

```
mcp_test/
├── ai-front-end/        # 前端Vue项目
│   ├── public/          # 静态资源
│   ├── src/             # 源代码
│   │   ├── assets/      # 资源文件
│   │   ├── components/  # 组件
│   │   ├── pages/       # 页面组件
│   │   ├── App.vue      # 根组件
│   │   └── main.js      # 入口文件
│   ├── package.json     # 前端依赖
│   └── vite.config.js   # Vite配置
├── boot-mcp-server/     # 后端Spring Boot项目
│   ├── src/             # 后端源码
│   │   ├── main/        # 主要代码
│   │   └── test/        # 测试代码
│   └── pom.xml          # Maven依赖配置
```

## 页面功能说明

### AI对话页面
- 支持创建、切换、重命名和删除会话
- 实时流式接收AI回复
- 支持在对话中上传文件
- 历史消息自动保存和加载

### 知识库页面
- 提供基于向量检索的语义搜索
- 显示搜索结果和相关度评分
- 支持高亮匹配内容

### 文件上传页面
- 支持多种格式文件上传
- 文件内容自动处理并导入知识库
- 上传状态和结果反馈

### 用户认证页面
- 用户注册和登录功能
- 基于Token的身份验证
- 受保护路由的访问控制

## 开发环境设置

### 前端开发

1. 安装依赖
```sh
cd ai-front-end
npm install
```

2. 启动开发服务器
```sh
npm run dev
```

3. 构建生产版本
```sh
npm run build
```

4. 预览生产构建
```sh
npm run preview
```

### 后端开发

1. 配置数据库连接
2. 配置Redis连接
3. 配置AI模型参数
4. 启动Spring Boot应用

## API接口说明

### 认证相关
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/register` - 用户注册

### 对话相关
- `GET /api/chat/sessions` - 获取会话列表
- `POST /api/chat/session` - 创建新会话
- `GET /api/chat/session/{id}` - 获取会话消息
- `POST /api/chat/session/{id}/message` - 发送消息

### 知识库相关
- `POST /api/knowledge-base/upload` - 上传文件到知识库
- `POST /api/knowledge-base/ask` - 知识库查询

### ETL相关
- `POST /api/etl/upload` - 上传并处理文件

## 部署说明

### 前端部署
1. 构建生产版本
```sh
npm run build
```

2. 将`dist`目录部署到Nginx或其他Web服务器

### 后端部署
1. 打包为JAR文件
```sh
mvn clean package
```

2. 运行JAR文件
```sh
java -jar boot-mcp-server-0.0.1-SNAPSHOT.jar
```

## 注意事项

1. 确保Redis服务正常运行
2. 确保MySQL数据库已正确配置
3. 配置文件中的AI模型参数需要根据实际环境调整
4. 开发环境中需配置跨域访问

## 许可证

本项目采用MIT许可证。
