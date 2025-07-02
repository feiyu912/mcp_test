package com.mcpserver.service;

import com.mcpserver.entity.User;

public interface UserService {
    User findByUsername(String username);
    boolean register(String username, String password);
    User login(String username, String password);
} 