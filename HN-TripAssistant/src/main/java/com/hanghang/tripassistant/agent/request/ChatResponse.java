package com.hanghang.tripassistant.agent.request;

import com.hanghang.tripassistant.agent.intent.IntentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "聊天响应")
public class ChatResponse {
    @Schema(description = "会话ID；前端始终带回来，保证多轮对话同一会话")
    private String sessionId;

    @Schema(description = "本轮识别出的意图")
    private IntentType intent;

    @Schema(description = "自然语言回复，任何情况下都应有值")
    private String reply;

    @Schema(description = "追问文案；编排类意图缺参时填写")
    private String askMessage;

    @Schema(description = "编排类意图专用：参数是否齐全")
    private boolean complete;

    @Schema(description = "结构化数据，如 tripPlan/flights/weather/pois")
    private Object data;
}
