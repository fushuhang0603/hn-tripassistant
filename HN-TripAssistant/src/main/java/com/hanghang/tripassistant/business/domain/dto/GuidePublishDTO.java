package com.hanghang.tripassistant.business.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "发布攻略")
public class GuidePublishDTO {

    @Schema(description = "标题")
    private String title;

    @Schema(description = "封面图URL，为空时使用默认图")
    private String cover;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "行程时长，如 3天2晚")
    private String days;

    @Schema(description = "预算，如 ¥3000")
    private String budget;

    @Schema(description = "标签列表，如 [亲子游, 海边度假]")
    private List<String> tags;

    @Schema(description = "正文，纯文本多段落")
    private String content;

    @Schema(description = "图集URL列表，最多9张，文末展示")
    private List<String> images;

    @Schema(description = "来源：AI行程一键发布时传 AI_TRIP，用户手动发布不传")
    private String source;

    @Schema(description = "AI行程快照JSON，仅AI一键发布时携带")
    private Object tripPlan;

}
