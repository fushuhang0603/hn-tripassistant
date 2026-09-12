package com.hanghang.tripassistant.service.impl;

import com.hanghang.tripassistant.agent.context.SessionManager;
import com.hanghang.tripassistant.agent.context.SessionPhase;
import com.hanghang.tripassistant.agent.handler.HandlerRegistry;
import com.hanghang.tripassistant.agent.handler.HandlerResult;
import com.hanghang.tripassistant.agent.handler.IntentHandler;
import com.hanghang.tripassistant.agent.intent.IntentResult;
import com.hanghang.tripassistant.agent.intent.IntentType;
import com.hanghang.tripassistant.agent.recognizer.IntentRecognizer;
import com.hanghang.tripassistant.agent.request.ChatContext;
import com.hanghang.tripassistant.agent.request.ChatRequest;
import com.hanghang.tripassistant.agent.request.StreamEvent;
import com.hanghang.tripassistant.business.utils.UserContext;
import com.hanghang.tripassistant.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 对话编排服务：会话加载 → 意图识别 → Handler 分发 → 会话保存 → 响应组装。
 * Controller 只做 HTTP 收口，全部编排逻辑集中在此。
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {
    /**
     * Handler 注册表，负责按意图分发 Handler
     */
    @Autowired
    private HandlerRegistry handlerRegistry;
    /**
     * 意图识别器：规则段 + LLM 段
     */
    @Autowired
    private IntentRecognizer intentRecognizer;
    /**
     * 会话管理：Redis 快照加载 / 槽位合并 / 保存 / 取消
     */
    @Autowired
    private SessionManager sessionManager;

    @Override
    public Flux<StreamEvent> chatStream(ChatRequest request) {
        // 1. 会话加载
        String sessionId = resolveSessionId(request);
        ChatContext context = sessionManager.loadOrCreate(sessionId, UserContext.get());
        context.setMessage(request.getMessage());

        // 2. 意图识别：规则短路 → LLM 分类 → 低置信度降级 GENERAL
        IntentResult intentResult = intentRecognizer.recognize(request.getMessage(), context);
        context.setIntentResult(intentResult);

        // 3. 取消语义：清空会话快照，友好回复
        if (intentResult.isCancel()) {
            sessionManager.cancel(sessionId);
            return Flux.just(
                    StreamEvent.meta(sessionId, IntentType.GENERAL.name()),
                    StreamEvent.token("好的，已帮你取消～还有什么想聊的，随时找小岛民～"));
        }

        // 4. 回退路由：COLLECTING（缺参追问）/ PLAN_DONE（行程调整）阶段用户回答时，沿用快照原意图
        resolveFallbackIntent(context);

        // 5. 槽位合并：历史快照 + 本轮提取，新值覆盖、null 不覆盖
        Map<String, Object> mergedSlots = sessionManager.mergeSlots(context.getExtractParam(), intentResult.getSlots());
        intentResult.setSlots(mergedSlots);
        context.setExtractParam(mergedSlots);

        // 6. 分发：先发 meta 事件（含 sessionId，前端持久化带回），再订阅 Handler 事件流
        IntentHandler handler = handlerRegistry.dispatch(intentResult.getIntent());
        StreamEvent meta = StreamEvent.meta(sessionId, handler.support().name());

        // 7. 流结束保存：边推边收集 token 拼完整回复、ask 事件记追问、end 事件记结构化数据，流结束统一落库
        StringBuilder replyBuf = new StringBuilder();
        AtomicReference<String> askRef = new AtomicReference<>();
        AtomicReference<Object> dataRef = new AtomicReference<>();
        return Flux.concat(Flux.just(meta), handler.handleStream(context))
                .doOnNext(event -> {
                    if ("token".equals(event.getType()) && event.getContent() != null) {
                        replyBuf.append(event.getContent());
                    } else if ("ask".equals(event.getType())) {
                        askRef.set(event.getAskMessage());
                    } else if ("end".equals(event.getType()) && event.getData() != null) {
                        dataRef.set(event.getData());
                    }
                })
                .doOnComplete(() -> saveStream(context, replyBuf.toString(), askRef.get(), dataRef.get()))
                .doOnError(e -> saveStream(context, replyBuf.toString(), askRef.get(), dataRef.get()));
    }

    /** 会话ID：前端带回优先，否则新建 */
    private String resolveSessionId(ChatRequest request) {
        return StringUtils.hasText(request.getSessionId())
                ? request.getSessionId()
                : UUID.randomUUID().toString().replace("-", "");
    }

    /** 回退路由：COLLECTING（缺参追问）或 PLAN_DONE（行程调整）阶段识别为 GENERAL 时，回退快照原意图 */
    private void resolveFallbackIntent(ChatContext context) {
        IntentResult intentResult = context.getIntentResult();
        boolean waitingPhase = context.getPhase() == SessionPhase.COLLECTING
                || context.getPhase() == SessionPhase.PLAN_DONE;
        if (waitingPhase
                && intentResult.getIntent() == IntentType.GENERAL
                && context.getIntent() != null) {
            log.info("[回退路由] 会话处于 {}，识别 GENERAL，回退原意图={}", context.getPhase(), context.getIntent());
            intentResult.setIntent(context.getIntent());
        }
    }

    /** 流式结束统一保存：拼接 token 为完整回复，ask 文案还原追问态，end 数据落行程快照 */
    private void saveStream(ChatContext context, String reply, String askMessage, Object data) {
        HandlerResult result = new HandlerResult();
        result.setReply(reply);
        result.setAskMessage(askMessage);
        result.setComplete(true);
        result.setData(data);
        sessionManager.save(context, result);
    }
}
