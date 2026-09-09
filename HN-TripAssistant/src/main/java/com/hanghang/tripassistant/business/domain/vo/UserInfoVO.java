package com.hanghang.tripassistant.business.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UserInfoVO {
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "用户ID")
    private Long id;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "用户状态 1-启用 0-禁用")
    private Integer status;
    @Schema(description = "角色")
    private String role;

}
