package com.mcpserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mcpserver.entity.ChatMessage;
import com.mcpserver.entity.ChatSession;
import com.mcpserver.mapper.ChatMessageMapper;
import com.mcpserver.mapper.ChatSessionMapper;
import com.mcpserver.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {
    @Autowired
    private ChatMessageMapper chatMessageMapper;
    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Override
    public List<ChatMessage> getMessagesBySessionId(Long sessionId, Long userId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) return List.of();
        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId).orderByAsc("created_at");
        return chatMessageMapper.selectList(wrapper);
    }

    @Override
    public boolean addMessage(Long sessionId, String role, String content, String reference) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setReference(reference);
        return chatMessageMapper.insert(msg) > 0;
    }

    @Override
    public void deleteMessagesBySessionId(Long sessionId) {
        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId);
        chatMessageMapper.delete(wrapper);
    }
} 