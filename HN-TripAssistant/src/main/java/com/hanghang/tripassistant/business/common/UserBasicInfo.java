package com.hanghang.tripassistant.business.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserBasicInfo {

    private Long id;

    private String username;

    private String role;

}
