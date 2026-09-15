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
@Schema(description = "攻略实体类")
public class Guide {

    @Schema(description = "攻略ID")
    private Long id;

    @Schema(description = "作者用户ID")
    private Long userId;

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

    @Schema(description = "标签，逗号分隔，如 亲子游,海边度假")
    private String tags;

    @Schema(description = "正文，纯文本多段落")
    private String content;

    @Schema(description = "来源：USER=用户发布 AI_TRIP=AI行程一键发布")
    private String source;

    @Schema(description = "AI行程快照JSON，用户发布时为null")
    private String tripPlan;

    @Schema(description = "点赞数")
    private Integer likes;

    @Schema(description = "状态：1=已发布 0=下架")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "作者昵称")
    private String authorName;

}
