package com.hanghang.tripassistant.agent.context;

import com.hanghang.tripassistant.agent.intent.IntentType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 会话快照：Redis chat:session:{sessionId} 的 JSON 结构。
 * 承载多轮对话的状态记忆：当前阶段、原意图、已收集槽位、行程结果。
 */
@Data
@NoArgsConstructor
public class ChatSession {

    /** 会话ID */
    private String sessionId;

    /** 归属用户；未登录为 null */
    private Long userId;

    /** 会话阶段，默认空闲 */
    private SessionPhase phase = SessionPhase.IDLE;

    /** 追问态（COLLECTING）时的原意图，用于追问回退路由 */
    private IntentType intent;

    /** 槽位快照：参数合并后的结果（cities/days/travelDate 等） */
    private Map<String, Object> extractParam;

    /** 行程快照，行程编排 Agent 接入后使用 */
    private Object tripPlan;

    /** 最近更新时间 */
    private LocalDateTime updateTime;
}
