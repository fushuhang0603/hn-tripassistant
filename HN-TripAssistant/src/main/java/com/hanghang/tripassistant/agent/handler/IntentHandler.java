package com.hanghang.tripassistant.agent.handler;

import com.hanghang.tripassistant.agent.intent.IntentType;
import com.hanghang.tripassistant.agent.request.ChatContext;
import com.hanghang.tripassistant.agent.request.StreamEvent;
import reactor.core.publisher.Flux;

/**
 * 意图处理器策略接口（纯流式）。
 * 新增能力 = 新增一个实现类并声明 @Component，
 * HandlerRegistry 启动时自动收集，主链路零改动。
 */
public interface IntentHandler {

    /**
     * 声明本 Handler 承接的意图。
     * 一个意图只能有一个 Handler；GENERAL 作为兜底必须存在。
     */
    IntentType support();

    /**
     * 流式执行（SSE）。
     *
     * @param context 含 sessionId/用户/原始消息/识别结果
     * @return 事件流：token 增量 + 可选 end/ask/error 终态事件
     */
    Flux<StreamEvent> handleStream(ChatContext context);
}
