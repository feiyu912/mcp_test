package com.mcpserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mcpserver.entity.ChatSession;
import java.util.List;

public interface ChatSessionService  {
    List<ChatSession> getSessionsByUserId(Long userId);
    ChatSession createSession(Long userId, String title);
    boolean renameSession(Long sessionId, Long userId, String title);
    boolean deleteSession(Long sessionId, Long userId);
} 