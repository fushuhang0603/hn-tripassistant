package com.hanghang.tripassistant.service;

import com.hanghang.tripassistant.business.domain.dto.LoginDTO;
import com.hanghang.tripassistant.business.domain.dto.RegisterDTO;
import com.hanghang.tripassistant.business.domain.vo.LoginVO;

public interface UserService {

    LoginVO login(LoginDTO loginDTO);

    void register(RegisterDTO registerDTO);
}
