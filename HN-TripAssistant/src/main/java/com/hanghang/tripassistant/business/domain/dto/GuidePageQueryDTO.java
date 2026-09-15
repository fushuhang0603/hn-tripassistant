package com.hanghang.tripassistant.business.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "攻略广场列表查询参数")
public class GuidePageQueryDTO {

    @Schema(description = "城市筛选，如 三亚")
    private String city;

    @Schema(description = "标签筛选，如 亲子游")
    private String tag;

    @Schema(description = "关键词搜索，匹配标题/标签/正文")
    private String keyword;

    @Schema(description = "排序：hot=按点赞数 new=按创建时间，默认 new")
    private String sort;

    @Schema(description = "页码，从1开始")
    private Integer page = 1;

    @Schema(description = "每页条数")
    private Integer size = 20;

}
