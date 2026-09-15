package com.hanghang.tripassistant.business.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "通用分页返回")
public class PageResult<T> {

    @Schema(description = "当前页数据")
    private List<T> records;

    @Schema(description = "总条数")
    private Long total;

}
