package com.hanghang.tripassistant.agent.request;

import com.hanghang.tripassistant.agent.context.ChatMessage;
import com.hanghang.tripassistant.agent.context.SessionPhase;
import com.hanghang.tripassistant.agent.intent.IntentResult;
import com.hanghang.tripassistant.agent.intent.IntentType;
import com.hanghang.tripassistant.business.common.UserBasicInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

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

    @Schema(description = "会话阶段，来自 Redis 快照")
    private SessionPhase phase;

    @Schema(description = "追问态时的原意图，来自 Redis 快照，用于追问回退路由")
    private IntentType intent;

    @Schema(description = "历史槽位快照，参数合并后的结果")
    private Map<String, Object> extractParam;

    @Schema(description = "已定稿的行程快照，来自 Redis 业务快照，调整行程时回填")
    private Object tripPlan;

    @Schema(description = "消息历史，时间正序（旧 → 新）")
    private List<ChatMessage> history;
}
