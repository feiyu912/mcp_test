package com.mcpserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mcpserver.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT * FROM user WHERE username = #{username} LIMIT 1")
    User selectByUsername(String username);
} 