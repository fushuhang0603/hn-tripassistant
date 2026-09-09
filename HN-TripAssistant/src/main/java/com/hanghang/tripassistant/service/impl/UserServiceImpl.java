package com.hanghang.tripassistant.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.hanghang.tripassistant.business.common.BusinessException;
import com.hanghang.tripassistant.business.common.Constant;
import com.hanghang.tripassistant.business.domain.dto.LoginDTO;
import com.hanghang.tripassistant.business.domain.dto.RegisterDTO;
import com.hanghang.tripassistant.business.domain.entity.User;
import com.hanghang.tripassistant.business.domain.vo.LoginVO;
import com.hanghang.tripassistant.business.domain.vo.UserInfoVO;
import com.hanghang.tripassistant.business.utils.JwtUtil;
import com.hanghang.tripassistant.mapper.UserMapper;
import com.hanghang.tripassistant.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        if (StrUtil.isBlank(loginDTO.getUsername())) {
            throw new BusinessException("用户名不能为空");
        }
        if (StrUtil.isBlank(loginDTO.getPassword())) {
            throw new BusinessException("密码不能为空");
        }
        User user = userMapper.getByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BusinessException("用户已被禁用");
        }
        if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        UserInfoVO userInfoVO = BeanUtil.copyProperties(user, UserInfoVO.class);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        return LoginVO.builder()
                .token(token)
                .userInfoVO(userInfoVO)
                .build();
    }

    @Override
    public void register(RegisterDTO registerDTO) {
        if (StrUtil.isBlank(registerDTO.getUsername())) {
            throw new BusinessException("用户名不能为空");
        }
        if (StrUtil.isBlank(registerDTO.getPassword())) {
            throw new BusinessException("密码不能为空");
        }
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (userMapper.getByUsername(registerDTO.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }

        User user = User.builder()
                .username(registerDTO.getUsername())
                .password(BCrypt.hashpw(registerDTO.getPassword()))
                .role(Constant.ROLE_USER)
                .status(Constant.USER_ENABLE)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        userMapper.insert(user);
    }
}
