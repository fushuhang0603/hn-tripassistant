package com.hanghang.tripassistant.business.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginVO {
    @Schema(description = "token")
    private String token;
    @Schema(description = "用户信息")
    private UserInfoVO userInfoVO;

}
