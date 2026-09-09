package com.hanghang.tripassistant.agent.graph.state;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用于提取用户提供基本出行信息的参数
 */
@Data
@Schema(description = "提取参数")
public class ExtractParamState {
    @Schema(description = "旅行天数")
    private String days;

    @Schema(description = "旅行城市")
    private List<String> cities;

    @Schema(description = "旅行人数")
    private String person;

    @Schema(description = "出行方式")
    private String travelType;

    @Schema(description = "旅游预算")
    private String budget;

    @Schema(description = "出发地，例如深圳、广州")
    private String departure;

    @Schema(description = "出发日期，例如2026-10-01")
    private String travelDate;

    // 下面全部是可选字段，不主动追问，识别到才填充
    @Schema(description = "人群标签：情侣蜜月、亲子带娃、老人休闲、朋友结伴")
    private List<String> crowdTags;

    @Schema(description = "游玩偏好标签：躺平度假、出海海岛、美食探店、免税购物、小众打卡")
    private List<String> preferenceTags;

    @Schema(description = "需要避开的项目，例如不要出海、不要人多景点")
    private List<String> exclude;

    @Schema(description = "出行月份，1‑12，没有识别到为null")
    private String travelMonth;

    @Schema(description = "用户额外特殊要求")
    private String remark;
}
