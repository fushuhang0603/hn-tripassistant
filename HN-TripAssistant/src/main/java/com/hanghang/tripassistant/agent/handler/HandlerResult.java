package com.hanghang.tripassistant.agent.handler;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Handler执行结果")
public class HandlerResult {
    @Schema(description = "自然语言回复，任何情况下都应有值")
    private String reply;

    @Schema(description = "追问文案；编排类意图缺参时填写，触发前端继续提问")
    private String askMessage;

    @Schema(description = "编排类意图专用：参数是否齐全；非编排类恒为 true")
    private boolean complete;

    @Schema(description = "结构化数据，如 tripPlan/flights/weather/pois，供前端渲染卡片")
    private Object data;

}
