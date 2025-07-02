package com.mcpserver.service.impl;

import com.mcpserver.entity.User;
import com.mcpserver.mapper.UserMapper;
import com.mcpserver.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public User findByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public boolean register(String username, String password) {
        if (findByUsername(username) != null) return false;
        User user = new User();
        user.setUsername(username);
        user.setPassword(password); // 生产环境请加密
        return userMapper.insert(user) > 0;
    }

    @Override
    public User login(String username, String password) {
        User user = findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }
} 