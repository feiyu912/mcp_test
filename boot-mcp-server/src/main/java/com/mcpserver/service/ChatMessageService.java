package com.mcpserver.service;

import com.mcpserver.entity.ChatMessage;
import java.util.List;

public interface ChatMessageService {
    boolean addMessage(Long sessionId, String role, String content, String reference);
    List<ChatMessage> getMessagesBySessionId(Long sessionId, Long userId);
    boolean addMessage(Long sessionId, String role, String content, String reference, String mcpContent);
    void deleteMessagesBySessionId(Long sessionId);
} 