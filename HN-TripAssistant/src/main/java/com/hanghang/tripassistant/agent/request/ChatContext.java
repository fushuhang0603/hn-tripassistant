package com.hanghang.tripassistant.agent.request;

import com.hanghang.tripassistant.agent.intent.IntentResult;
import com.hanghang.tripassistant.business.common.UserBasicInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "聊天上下文")
public class ChatContext {
    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "当前登录用户，未登录为 null")
    private UserBasicInfo user;

    @Schema(description = "用户本轮输入的原始消息")
    private String message;

    @Schema(description = "意图识别结果")
    private IntentResult intentResult;
}
