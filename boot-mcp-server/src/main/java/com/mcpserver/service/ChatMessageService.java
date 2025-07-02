package com.mcpserver.service;

import com.mcpserver.entity.ChatMessage;
import java.util.List;

public interface ChatMessageService {
    List<ChatMessage> getMessagesBySessionId(Long sessionId, Long userId);
    boolean addMessage(Long sessionId, String role, String content);
    void deleteMessagesBySessionId(Long sessionId);
} 