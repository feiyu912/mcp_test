package com.mcpserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mcpserver.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
} 