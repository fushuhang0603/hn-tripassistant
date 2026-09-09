package com.hanghang.tripassistant.mapper;


import com.hanghang.tripassistant.business.domain.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    int insert(User user);

    User getByUsername(String username);
}
