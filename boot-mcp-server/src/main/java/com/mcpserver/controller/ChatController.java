package com.mcpserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpserver.entity.ChatSession;
import com.mcpserver.entity.ChatMessage;
import com.mcpserver.service.ChatSessionService;
import com.mcpserver.service.ChatMessageService;
import com.mcpserver.service.ChatSessionFileService;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.ai.vectorstore.VectorStore;
import com.mcpserver.service.EmbeddingUtils;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import com.mcpserver.mcp.impl.TimeTool;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    @Autowired
    private ChatSessionService chatSessionService;
    @Autowired
    private ChatMessageService chatMessageService;
    @Autowired
    private ChatSessionFileService chatSessionFileService;
    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private ChatModel chatModel;
    @Autowired
    private TimeTool timeTool;
    @Autowired
    private MethodToolCallbackProvider mcpToolCallbacks;

    // 获取当前用户所有会话
    @GetMapping("/sessions")
    public List<ChatSession> getSessions(@RequestHeader("Authorization") String token) {
        Long userId = UserController.getUserIdByToken(token);
        return chatSessionService.getSessionsByUserId(userId);
    }

    // 获取某会话所有消息
    @GetMapping("/session/{id}")
    public List<ChatMessage> getSessionMessages(@RequestHeader("Authorization") String token, @PathVariable("id") Long sessionId) {
        // 兼容历史数据：只要 sessionId 存在就查
        ChatSession session = chatSessionService.getSessionById(sessionId);
        if (session == null) return Collections.emptyList();
        return chatMessageService.getMessagesBySessionId(sessionId, session.getUserId());
    }

    // 向会话发送消息
    @PostMapping("/session/{id}/message")
    public Map<String, Object> sendMessage(@RequestHeader("Authorization") String token, @PathVariable("id") Long sessionId, @RequestBody Map<String, Object> body) {
        Long userId = UserController.getUserIdByToken(token);
        String role = (String) body.get("role");
        String content = (String) body.get("content");
        String reference = body.get("reference") != null ? body.get("reference").toString() : null;
        if (reference == null) reference = "[]";
        System.out.println("[DEBUG] sendMessage reference: " + reference);
        // 校验会话归属
        List<ChatSession> sessions = chatSessionService.getSessionsByUserId(userId);
        boolean hasSession = sessions.stream().anyMatch(s -> s.getId().equals(sessionId));
        if (!hasSession) return Map.of("success", false, "msg", "无权访问该会话");
        boolean ok = chatMessageService.addMessage(sessionId, role, content, reference);
        if (ok) return Map.of("success", true);
        return Map.of("success", false, "msg", "消息发送失败");
    }

    // 新建会话
    @PostMapping("/session")
    public Map<String, Object> createSession(@RequestHeader("Authorization") String token, @RequestBody Map<String, String> body) {
        Long userId = UserController.getUserIdByToken(token);
        String title = body.getOrDefault("title", "新会话");
        ChatSession session = chatSessionService.createSession(userId, title);
        if (session != null) return Map.of("success", true, "id", session.getId());
        return Map.of("success", false, "msg", "新建会话失败");
    }

    // 会话重命名
    @PostMapping("/session/{id}/rename")
    public Map<String, Object> renameSession(@RequestHeader("Authorization") String token, @PathVariable("id") Long sessionId, @RequestBody Map<String, String> body) {
        Long userId = UserController.getUserIdByToken(token);
        String title = body.getOrDefault("title", "");
        boolean ok = chatSessionService.renameSession(sessionId, userId, title);
        if (ok) return Map.of("success", true);
        return Map.of("success", false, "msg", "重命名失败");
    }

    // 会话删除
    @DeleteMapping("/session/{id}")
    public Map<String, Object> deleteSession(@RequestHeader("Authorization") String token, @PathVariable("id") Long sessionId) {
        Long userId = UserController.getUserIdByToken(token);
        boolean ok = chatSessionService.deleteSession(sessionId, userId);
        if (ok) {
            chatMessageService.deleteMessagesBySessionId(sessionId);
            // 同步删除 session embedding
            // 1. 查全部 embedding
            SearchRequest request = SearchRequest.builder().query("").topK(1000).build();
            List<Document> allDocs = vectorStore.similaritySearch(request);
            List<String> toDelete = new ArrayList<>();
            for (Document doc : allDocs) {
                if ("session".equals(doc.getMetadata().get("type")) &&
                    String.valueOf(sessionId).equals(doc.getMetadata().get("sessionId"))) {
                    if (doc.getId() != null) {
                        toDelete.add(doc.getId());
                    }
                }
            }
            if (!toDelete.isEmpty()) {
                vectorStore.delete(toDelete);
            }
            return Map.of("success", true);
        }
        return Map.of("success", false, "msg", "删除失败");
    }

    // 对话专属文件上传（embedding后存入redis，不进全局知识库）
    @PostMapping("/session/{sessionId}/upload")
    public ResponseEntity<?> uploadSessionFile(@RequestHeader("Authorization") String token,
                                               @PathVariable("sessionId") Long sessionId,
                                               @RequestParam("file") MultipartFile file) {
        Long userId = UserController.getUserIdByToken(token);
        // 校验会话归属
        List<ChatSession> sessions = chatSessionService.getSessionsByUserId(userId);
        boolean hasSession = sessions.stream().anyMatch(s -> s.getId().equals(sessionId));
        if (!hasSession) return ResponseEntity.status(403).body("无权访问该会话");
        try {
            // 1. 解析文件内容（如txt/pdf/doc等）
            // 2. 调用embedding服务，得到embedding向量
            // 3. 存入redis，key=chat:session:{sessionId}:embeddings，value=embedding+原文+文件名等
            chatSessionFileService.saveSessionFileEmbedding(sessionId, file);
            return ResponseEntity.ok("上传成功");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("上传失败: " + e.getMessage());
        }
    }

    // 对话检索接口：合并全局知识库和session embedding
    @PostMapping("/session/{sessionId}/search")
    public List<Map<String, Object>> searchSessionAndGlobal(@PathVariable("sessionId") Long sessionId, @RequestBody String query) {
        List<float[]> queryVecs = embeddingModel.embed(List.of(query));
        float[] queryVec = queryVecs.get(0);
        List<Map<String, Object>> results = new ArrayList<>();
        FilterExpressionBuilder global = new FilterExpressionBuilder();
        // 检索全局知识库（type = global）
        FilterExpressionBuilder.Op globalOp = global.eq("type", "global");
        SearchRequest globalRequest = SearchRequest.builder()
            .query(query)
            .topK(5)
            .filterExpression(globalOp.build())
            .build();
        List<Document> globalDocs = vectorStore.similaritySearch(globalRequest);
        System.out.println("[DEBUG] global filter: " + globalOp + ", docs found: " + globalDocs.size());
        for (Document doc : globalDocs) {
            Map<String, Object> map = new HashMap<>();
            map.put("source", "global");
            map.put("text", doc.getText());
            map.put("score", doc.getScore() != null ? doc.getScore() : 0.0);
            results.add(map);
        }
        // 检索session embedding，兼容sessionId为字符串和数字
        FilterExpressionBuilder.Op sessionOp = global.and(
                global.eq("type", "session"),
                global.or(
                        global.eq("sessionId", String.valueOf(sessionId)),
                        global.eq("sessionId", sessionId)
            )
        );
        SearchRequest sessionRequest = SearchRequest.builder()
            .query(query)
            .topK(5)
            .filterExpression(sessionOp.build())
            .build();
        List<Document> sessionDocs = vectorStore.similaritySearch(sessionRequest);
        System.out.println("[DEBUG] session filter: " + sessionOp + ", docs found: " + sessionDocs.size());
        // 如果查不到，再降级只查 type == 'session'
        if (sessionDocs.isEmpty()) {
            FilterExpressionBuilder.Op fallbackOp = global.eq("type", "session");
            SearchRequest fallbackRequest = SearchRequest.builder()
                .query(query)
                .topK(5)
                .filterExpression(fallbackOp.build())
                .build();
            sessionDocs = vectorStore.similaritySearch(fallbackRequest);
            System.out.println("[DEBUG] fallback session filter: " + fallbackOp + ", docs found: " + sessionDocs.size());
        }
        for (Document doc : sessionDocs) {
            Map<String, Object> map = new HashMap<>();
            map.put("source", "session");
            map.put("text", doc.getText());
            map.put("fileName", doc.getMetadata().get("fileName"));
            map.put("score", doc.getScore() != null ? doc.getScore() : 0.0);
            results.add(map);
        }
        results.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
        return results.size() > 10 ? results.subList(0, 10) : results;
    }

    // 对话流式回答接口：RAG+Tool Calling
    @PostMapping(value = "/session/{sessionId}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatWithSessionAndGlobal(
            @PathVariable("sessionId") Long sessionId,
            @RequestBody Map<String, Object> body
    ) {
        String prompt = (String) body.get("prompt");
        System.out.println("[DEBUG] 收到用户问题: " + prompt);

        // 1. 检索上下文
        List<Map<String, Object>> contextList = searchSessionAndGlobal(sessionId, prompt);

        // 2. 组装 PromptTemplate，补全所有 MCP 工具描述
        StringBuilder contextBuilder = new StringBuilder();
        for (Map<String, Object> ctx : contextList) {
            contextBuilder.append(ctx.getOrDefault("text", ""));
            contextBuilder.append("\n");
        }
        String contextString = contextBuilder.toString().trim();
        
        // 改进的提示词模板，更清晰地说明工具调用
        String promptWithContext = """
你可以调用如下工具：
- time-mcp(): 获取当前时间
- random-mcp(min: int, max: int): 生成随机整数
- filesystem-mcp(path: string): 文件系统操作
- unitconvert-mcp(value: double, type: string): 单位换算

请根据用户输入自动决定是否需要调用工具，并输出结构化参数。

{query}
Context:
---------------------
{question_answer_context}
---------------------
""";

        PromptTemplate promptTemplate = PromptTemplate.builder()
                .template(promptWithContext)
                .variables(Map.of(
                    "query", prompt,
                    "question_answer_context", contextString
                ))
                .build();

        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .promptTemplate(promptTemplate)
                .build();

        ChatClient chatClient = ChatClient.builder(chatModel).build();
        final StringBuilder lastContent = new StringBuilder();
        final String safePrompt = prompt != null ? prompt : "";

        return chatClient.prompt()
                .user(safePrompt)
                .advisors(qaAdvisor)
                .toolCallbacks(mcpToolCallbacks) // 使用注入的MCP工具
                .stream()
                .content()
                .map(full -> {
                    String delta = full;
                    if (lastContent.length() > 0 && full.startsWith(lastContent.toString())) {
                        delta = full.substring(lastContent.length());
                    }
                    lastContent.setLength(0);
                    lastContent.append(full);
                    return ServerSentEvent.builder(delta).event("message").build();
                })
                .doOnComplete(() -> {
                    if (lastContent.length() > 0) {
                        String referenceJson = "[]";
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            referenceJson = mapper.writeValueAsString(contextList);
                        } catch (Exception e) {
                            System.out.println("[ERROR] 转换reference失败: " + e.getMessage());
                        }
                        // 自动提取所有MCP工具调用内容
                        String content = lastContent.toString();
                        // 根据前端传递的tools参数，只提取这些工具的"【工具】xxx-mcp:"块内容
                        List<String> tools = null;
                        try {
                            tools = (List<String>) body.get("tools");
                        } catch (Exception e) {
                            // 兼容前端未传tools的情况
                        }
                        StringBuilder mcpBuilder = new StringBuilder();
                        if (tools != null && !tools.isEmpty()) {
                            for (String tool : tools) {
                                String regex = "【工具】" + tool + ":\\s*([\\s\\S]*?)(?=【工具】[\\w\\-]+-mcp:|$)";
                                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
                                java.util.regex.Matcher matcher = pattern.matcher(content);
                                while (matcher.find()) {
                                    String mcp = matcher.group(1).trim();
                                    if (!mcp.isEmpty()) {
                                        if (mcpBuilder.length() > 0) mcpBuilder.append("\n\n");
                                        mcpBuilder.append(mcp);
                                    }
                                }
                            }
                        }
                        String mcpContent = mcpBuilder.length() > 0 ? mcpBuilder.toString() : null;
                        System.out.println("[DEBUG] AI原始回复: " + content);
                        System.out.println("[DEBUG] 提取到的MCP内容: " + mcpContent);
                        String contentWithoutMcp = content;
                        chatMessageService.addMessage(sessionId, "assistant", contentWithoutMcp, referenceJson, mcpContent);
                    }
                });
    }

    // 极简Tool Calling测试接口，测试MCP工具调用
    @PostMapping("/test/tool-calling")
    public String testToolCalling() {
        System.out.println("[DEBUG] timeTool bean class: " + timeTool.getClass());
        for (var m : timeTool.getClass().getMethods()) {
            System.out.println("[DEBUG] method: " + m.getName());
            var toolAnno = m.getAnnotation(org.springframework.ai.tool.annotation.Tool.class);
            System.out.println("[DEBUG] @Tool annotation: " + toolAnno);
        }
        
        String prompt = "请务必调用 time-mcp 工具获取当前时间，不要自己编造答案，只能用工具！";
        ChatClient chatClient = ChatClient.builder(chatModel).build();
        String result = chatClient.prompt()
                .user(prompt)
                .toolCallbacks(mcpToolCallbacks) // 使用注入的MCP工具
                .call().content();
        System.out.println("[TOOL CALLING DEMO] AI回复: " + result);
        return result;
    }

}
