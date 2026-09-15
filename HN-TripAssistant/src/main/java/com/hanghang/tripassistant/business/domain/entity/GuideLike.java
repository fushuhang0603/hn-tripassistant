package com.hanghang.tripassistant.business.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "攻略点赞记录实体类")
public class GuideLike {

    @Schema(description = "点赞记录ID")
    private Long id;

    @Schema(description = "点赞用户ID")
    private Long userId;

    @Schema(description = "攻略ID")
    private Long guideId;

    @Schema(description = "点赞时间")
    private LocalDateTime createTime;

}
