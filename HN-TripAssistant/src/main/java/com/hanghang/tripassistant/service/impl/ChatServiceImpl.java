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
import com.hanghang.tripassistant.agent.request.ChatResponse;
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
    public ChatResponse chat(ChatRequest request) {
        // 1. 会话加载：Redis 快照 → phase/原意图/槽位/历史；新会话由 SessionManager 兜底
        String sessionId = resolveSessionId(request);
        ChatContext context = sessionManager.loadOrCreate(sessionId, UserContext.get());
        context.setMessage(request.getMessage());

        // 2. 意图识别：规则短路 → LLM 分类 → 低置信度降级 GENERAL
        IntentResult intentResult = intentRecognizer.recognize(request.getMessage(), context);
        context.setIntentResult(intentResult);

        // 3. 取消语义：清空会话快照，友好回复
        if (intentResult.isCancel()) {
            sessionManager.cancel(sessionId);
            ChatResponse response = baseResponse(sessionId);
            response.setIntent(IntentType.GENERAL);
            response.setReply("好的，已帮你取消～还有什么想聊的，随时找小岛民～");
            response.setComplete(true);
            return response;
        }

        // 4. 追问态回退：COLLECTING 阶段用户回答追问（识别为 GENERAL）时，沿用快照原意图
        resolveCollectingIntent(context);

        // 5. 槽位合并：历史快照 + 本轮提取，新值覆盖、null 不覆盖
        Map<String, Object> mergedSlots = sessionManager.mergeSlots(context.getExtractParam(), intentResult.getSlots());
        intentResult.setSlots(mergedSlots);
        context.setExtractParam(mergedSlots);

        // 6. 分发执行
        IntentHandler handler = handlerRegistry.dispatch(intentResult.getIntent());
        HandlerResult result = handler.handle(context);

        // 7. 会话保存：快照 + 消息历史（内部兜底，Redis 故障不影响主链路）
        sessionManager.save(context, result);

        // 8. 组装统一响应
        ChatResponse response = baseResponse(sessionId);
        // 返回实际执行的意图：识别意图无对应 Handler 降级时，前端看到真实兜底结果
        response.setIntent(handler.support());
        response.setReply(result.getReply());
        response.setAskMessage(result.getAskMessage());
        response.setComplete(result.isComplete());
        response.setData(result.getData());
        return response;
    }

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

        // 4. 追问态回退：COLLECTING 阶段用户回答追问时，沿用快照原意图
        resolveCollectingIntent(context);

        // 5. 槽位合并：历史快照 + 本轮提取，新值覆盖、null 不覆盖
        Map<String, Object> mergedSlots = sessionManager.mergeSlots(context.getExtractParam(), intentResult.getSlots());
        intentResult.setSlots(mergedSlots);
        context.setExtractParam(mergedSlots);

        // 6. 分发：先发 meta 事件（含 sessionId，前端持久化带回），再订阅 Handler 事件流
        IntentHandler handler = handlerRegistry.dispatch(intentResult.getIntent());
        StreamEvent meta = StreamEvent.meta(sessionId, handler.support().name());

        // 7. 流结束保存：边推边收集 token 拼完整回复、ask 事件记追问，流结束统一落库
        StringBuilder replyBuf = new StringBuilder();
        AtomicReference<String> askRef = new AtomicReference<>();
        return Flux.concat(Flux.just(meta), handler.handleStream(context))
                .doOnNext(event -> {
                    if ("token".equals(event.getType()) && event.getContent() != null) {
                        replyBuf.append(event.getContent());
                    } else if ("ask".equals(event.getType())) {
                        askRef.set(event.getAskMessage());
                    }
                })
                .doOnComplete(() -> saveStream(context, replyBuf.toString(), askRef.get()))
                .doOnError(e -> saveStream(context, replyBuf.toString(), askRef.get()));
    }

    /** 会话ID：前端带回优先，否则新建 */
    private String resolveSessionId(ChatRequest request) {
        return StringUtils.hasText(request.getSessionId())
                ? request.getSessionId()
                : UUID.randomUUID().toString().replace("-", "");
    }

    /** 追问态回退：phase=COLLECTING 且本轮识别 GENERAL（非取消）时，沿用快照原意图继续追问流程 */
    private void resolveCollectingIntent(ChatContext context) {
        IntentResult intentResult = context.getIntentResult();
        if (context.getPhase() == SessionPhase.COLLECTING
                && intentResult.getIntent() == IntentType.GENERAL
                && context.getIntent() != null) {
            log.info("[追问回退] 会话处于 COLLECTING，识别 GENERAL，回退原意图={}", context.getIntent());
            intentResult.setIntent(context.getIntent());
        }
    }

    /** 流式结束统一保存：拼接 token 为完整回复，ask 文案还原追问态 */
    private void saveStream(ChatContext context, String reply, String askMessage) {
        HandlerResult result = new HandlerResult();
        result.setReply(reply);
        result.setAskMessage(askMessage);
        result.setComplete(true);
        sessionManager.save(context, result);
    }

    private ChatResponse baseResponse(String sessionId) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        return response;
    }
}
