package com.mcpserver.controller;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-base")
public class KnowledgeBaseController {
    private final ChatModel chatModel;
    private final VectorStore vectorStore;

    @Autowired
    public KnowledgeBaseController(ChatModel chatModel, VectorStore vectorStore) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    @PostMapping("/ask")
    public List<Document> askQuestion(@RequestBody String question) {
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(5)
                .build();
        return vectorStore.similaritySearch(request);
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStreamWithDatabase(@RequestBody String prompt) {
        String promptWithContext = """
{query}
下面是上下文信息
---------------------
{question_answer_context}
---------------------
给定的上下文和提供的历史信息，而不是事先的知识，回复用户的意见。如果答案不在上下文中，告诉用户你不能回答这个问题。
""";
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        // 用于记录上一次已推送内容的长度
        final StringBuilder lastContent = new StringBuilder();

        return chatClient.prompt()
                .user(prompt)
                .advisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .promptTemplate(new PromptTemplate(promptWithContext)).build())
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
                });
    }
} 