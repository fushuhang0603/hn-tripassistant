package com.hanghang.tripassistant.agent.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "聊天统一入口请求实体")
public class ChatRequest {
    @Schema(description = "会话ID；首次为空，后端创建后随响应返回，后续轮次原样带回")
    private String sessionId;

    @Schema(description = "用户本轮输入的原始消息")
    private String message;
}
