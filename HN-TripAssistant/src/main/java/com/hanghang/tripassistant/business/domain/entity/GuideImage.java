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
@Schema(description = "攻略图集实体类")
public class GuideImage {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "攻略ID")
    private Long guideId;

    @Schema(description = "图片完整URL")
    private String url;

    @Schema(description = "图集内顺序，从0开始")
    private Integer sort;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
