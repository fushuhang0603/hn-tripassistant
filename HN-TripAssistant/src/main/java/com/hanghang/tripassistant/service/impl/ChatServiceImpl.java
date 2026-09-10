package com.hanghang.tripassistant.service.impl;

import com.hanghang.tripassistant.agent.handler.HandlerRegistry;
import com.hanghang.tripassistant.agent.handler.HandlerResult;
import com.hanghang.tripassistant.agent.handler.IntentHandler;
import com.hanghang.tripassistant.agent.intent.IntentResult;
import com.hanghang.tripassistant.agent.recognizer.IntentRecognizer;
import com.hanghang.tripassistant.agent.request.ChatContext;
import com.hanghang.tripassistant.agent.request.ChatRequest;
import com.hanghang.tripassistant.agent.request.ChatResponse;
import com.hanghang.tripassistant.agent.request.StreamEvent;
import com.hanghang.tripassistant.business.utils.UserContext;
import com.hanghang.tripassistant.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * 对话编排服务：会话加载 → 意图识别 → Handler 分发 → 响应组装。
 * Controller 只做 HTTP 收口，全部编排逻辑集中在此。
 */
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

    @Override
    public ChatResponse chat(ChatRequest request) {
        // 1. 会话ID：Redis 未接入前临时生成；第 2 步由 SessionManager 接管（逻辑位置不变）
        String sessionId = StringUtils.hasText(request.getSessionId())
                ? request.getSessionId()
                : UUID.randomUUID().toString().replace("-", "");

        // 2. 构建上下文
        ChatContext context = new ChatContext();
        context.setSessionId(sessionId);
        context.setUser(UserContext.get());
        context.setMessage(request.getMessage());

        // 3. 意图识别：规则短路 → LLM 分类 → 低置信度降级 GENERAL
        IntentResult intentResult = intentRecognizer.recognize(request.getMessage(), context);
        context.setIntentResult(intentResult);

        // 4. 分发执行
        IntentHandler handler = handlerRegistry.dispatch(intentResult.getIntent());
        HandlerResult result = handler.handle(context);

        // 5. 组装统一响应
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
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
        // 1. 会话ID：Redis 未接入前临时生成；第 2 步由 SessionManager 接管（逻辑位置不变）
        String sessionId = StringUtils.hasText(request.getSessionId())
                ? request.getSessionId()
                : UUID.randomUUID().toString().replace("-", "");

        // 2. 构建上下文
        ChatContext context = new ChatContext();
        context.setSessionId(sessionId);
        context.setUser(UserContext.get());
        context.setMessage(request.getMessage());

        // 3. 意图识别：规则短路 → LLM 分类 → 低置信度降级 GENERAL
        IntentResult intentResult = intentRecognizer.recognize(request.getMessage(), context);
        context.setIntentResult(intentResult);

        // 4. 分发：先发 meta 事件（含 sessionId，前端持久化带回），再订阅 Handler 事件流
        IntentHandler handler = handlerRegistry.dispatch(intentResult.getIntent());
        StreamEvent meta = StreamEvent.meta(sessionId, handler.support().name());
        return Flux.concat(Flux.just(meta), handler.handleStream(context));
    }
}
