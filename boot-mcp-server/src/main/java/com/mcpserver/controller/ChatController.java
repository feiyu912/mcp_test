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

        // 1. 检索上下文
        List<Map<String, Object>> contextList = searchSessionAndGlobal(sessionId, prompt);
        StringBuilder context = new StringBuilder();
        for (Map<String, Object> item : contextList) {
            context.append(item.get("text")).append("\n");
        }

        // 2. 组装 PromptTemplate，补全所有 MCP 工具描述
        String promptWithContext = """
你可以调用如下工具：
- amap-mcp(address: string): 高德地图地理编码
- weather-mcp(city: string): 天气查询
- translate-mcp(text: string, target: string): 翻译
- time-mcp(): 获取当前时间
- random-mcp(min: int, max: int): 生成随机整数
- filesystem-mcp(path: string): 文件系统操作
- unitconvert-mcp(value: double, type: string): 单位换算
- stock-mcp(code: string): 股票查询

请根据用户输入自动决定是否需要调用工具，并输出结构化参数。

{query}
Context:
---------------------
{question_answer_context}
---------------------
""";

        PromptTemplate promptTemplate = PromptTemplate.builder()
                .template(promptWithContext)
                .build();

        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .promptTemplate(promptTemplate)
                .build();

        ChatClient chatClient = ChatClient.builder(chatModel).build();
        final StringBuilder lastContent = new StringBuilder();
        final String safePrompt = prompt != null ? prompt : "";
        final String safeContext = context != null ? context.toString() : "";

        return chatClient.prompt()
                .user(safePrompt)
                .advisors(qaAdvisor)
                .tools() // 自动注册所有 @Tool 工具
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
                        chatMessageService.addMessage(sessionId, "assistant", lastContent.toString(), referenceJson);
                    }
                });
    }

}
