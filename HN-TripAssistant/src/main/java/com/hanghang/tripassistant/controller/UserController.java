package com.hanghang.tripassistant.controller;


import com.hanghang.tripassistant.business.common.Result;
import com.hanghang.tripassistant.business.domain.dto.LoginDTO;
import com.hanghang.tripassistant.business.domain.dto.RegisterDTO;
import com.hanghang.tripassistant.business.domain.vo.LoginVO;
import com.hanghang.tripassistant.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO){
        log.info("用户登录: {}",loginDTO);
        LoginVO loginVO = userService.login(loginDTO);

        return Result.success(loginVO);
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterDTO registerDTO){
        log.info("用户注册: {}", registerDTO);
        userService.register(registerDTO);
        return Result.success();
    }

}
