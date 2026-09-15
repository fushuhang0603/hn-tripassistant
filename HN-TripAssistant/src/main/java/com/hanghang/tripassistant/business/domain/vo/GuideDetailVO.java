package com.hanghang.tripassistant.business.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "攻略详情")
public class GuideDetailVO {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "攻略ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "封面图URL")
    private String cover;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "行程时长，如 3天2晚")
    private String days;

    @Schema(description = "预算，如 ¥3000")
    private String budget;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "作者昵称")
    private String authorName;

    @Schema(description = "点赞数")
    private Integer likes;

    @Schema(description = "来源：USER=用户发布 AI_TRIP=AI行程一键发布")
    private String source;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;

    @Schema(description = "正文，纯文本多段落")
    private String content;

    @Schema(description = "图集URL列表，文末展示")
    private List<String> images;

    @Schema(description = "当前用户是否已点赞")
    private Boolean liked;

    @Schema(description = "AI行程快照JSON，用户发布的攻略为null")
    private Object tripPlan;

}
