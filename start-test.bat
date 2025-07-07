@echo off
echo ========================================
echo MCP工具调用测试启动脚本
echo ========================================

echo.
echo 1. 清理并重新编译后端项目...
cd boot-mcp-server
call mvn clean compile -q
if %errorlevel% neq 0 (
    echo 后端编译失败！
    pause
    exit /b 1
)

echo 2. 启动后端服务...
start "Spring Boot MCP Server" cmd /k "mvn spring-boot:run"

echo 3. 等待后端启动...
timeout /t 10 /nobreak > nul

echo 4. 启动前端服务...
cd ..\ai-front-end
start "Vue Frontend" cmd /k "npm run dev"

echo.
echo ========================================
echo 服务启动完成！
echo ========================================
echo 后端地址: http://localhost:8080
echo 前端地址: http://localhost:5173
echo.
echo 测试步骤：
echo 1. 打开浏览器访问 http://localhost:5173
echo 2. 注册/登录账号
echo 3. 点击左侧菜单"MCP测试"
echo 4. 测试各种工具调用功能
echo.
echo 按任意键退出...
pause > nul 