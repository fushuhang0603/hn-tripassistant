package com.hanghang.tripassistant.agent.context;

import com.hanghang.tripassistant.agent.handler.HandlerResult;
import com.hanghang.tripassistant.agent.request.ChatContext;
import com.hanghang.tripassistant.business.common.UserBasicInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话编排：主链路唯一入口，负责加载 / 槽位合并 / 保存 / 取消。
 * Redis 故障时降级为无记忆单轮，不阻塞对话主链路。
 */
@Slf4j
@Component
public class SessionManager {

    /** 注入 LLM 的历史窗口大小（与存储保留上限一致） */
    private static final int HISTORY_WINDOW = 30;

    private final SessionStore sessionStore;

    public SessionManager(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    /**
     * 加载或新建会话上下文：Redis 快照 → phase / 原意图 / 槽位快照 + 消息历史。
     * 快照不存在或 Redis 异常时按全新会话处理。
     */
    public ChatContext loadOrCreate(String sessionId, UserBasicInfo user) {
        ChatContext context = new ChatContext();
        context.setSessionId(sessionId);
        context.setUser(user);
        context.setPhase(SessionPhase.IDLE);
        context.setExtractParam(new HashMap<>());
        context.setHistory(List.of());
        try {
            ChatSession session = sessionStore.load(sessionId);
            if (session != null) {
                if (session.getPhase() != null) {
                    context.setPhase(session.getPhase());
                }
                context.setIntent(session.getIntent());
                if (session.getExtractParam() != null) {
                    context.setExtractParam(session.getExtractParam());
                }
                context.setHistory(sessionStore.recentMessages(sessionId, HISTORY_WINDOW));
            }
        } catch (Exception e) {
            log.error("[会话] 加载失败，退化为无记忆单轮 sessionId={}：{}", sessionId, e.getMessage());
        }
        return context;
    }

    /**
     * 参数合并：历史快照 + 本轮槽位，新值覆盖、null 不覆盖（追问不失忆的关键）。
     */
    public Map<String, Object> mergeSlots(Map<String, Object> history, Map<String, Object> current) {
        Map<String, Object> merged = new HashMap<>();
        if (history != null) {
            merged.putAll(history);
        }
        if (current != null) {
            current.forEach((key, value) -> {
                if (value != null) {
                    merged.put(key, value);
                }
            });
        }
        return merged;
    }

    /**
     * 保存会话：phase 迁移 + 写快照 + 追加本轮消息。
     * phase 规则：Handler 返回追问文案 → COLLECTING（记原意图供下轮回退）；否则回 IDLE。
     */
    public void save(ChatContext context, HandlerResult result) {
        try {
            ChatSession session = new ChatSession();
            session.setSessionId(context.getSessionId());
            session.setUserId(context.getUser() == null ? null : context.getUser().getId());
            session.setExtractParam(context.getExtractParam());
            session.setUpdateTime(LocalDateTime.now());
            if (StringUtils.hasText(result.getAskMessage())) {
                session.setPhase(SessionPhase.COLLECTING);
                session.setIntent(context.getIntentResult() == null ? null : context.getIntentResult().getIntent());
            } else {
                session.setPhase(SessionPhase.IDLE);
            }
            sessionStore.save(session);
            sessionStore.appendMessage(context.getSessionId(),
                    ChatMessage.of(MessageRole.USER, context.getMessage()));
            if (StringUtils.hasText(result.getReply())) {
                sessionStore.appendMessage(context.getSessionId(),
                        ChatMessage.of(MessageRole.ASSISTANT, result.getReply()));
            }
        } catch (Exception e) {
            log.error("[会话] 保存失败，本轮状态丢失 sessionId={}：{}", context.getSessionId(), e.getMessage());
        }
    }

    /**
     * 取消语义：清空快照与历史，会话回到全新状态。
     */
    public void cancel(String sessionId) {
        try {
            sessionStore.delete(sessionId);
        } catch (Exception e) {
            log.error("[会话] 取消清理失败 sessionId={}：{}", sessionId, e.getMessage());
        }
    }
}
