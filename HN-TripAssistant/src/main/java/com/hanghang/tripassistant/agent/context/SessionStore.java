package com.hanghang.tripassistant.agent.context;

import java.util.List;

/**
 * 会话存储抽象：快照 + 消息历史。
 * 与具体存储解耦，Redis 不可用时可替换本地实现，主链路无感。
 */
public interface SessionStore {

    /**
     * 读取会话快照；不存在或读取出错返回 null。
     */
    ChatSession load(String sessionId);

    /**
     * 写回会话快照并刷新 TTL。
     */
    void save(ChatSession session);

    /**
     * 追加一条消息到历史尾部（新消息在前），超出保留条数自动截断。
     */
    void appendMessage(String sessionId, ChatMessage message);

    /**
     * 读取最近 n 条消息，按时间正序返回（旧 → 新）。
     */
    List<ChatMessage> recentMessages(String sessionId, int n);

    /**
     * 删除会话快照与消息历史（取消语义使用）。
     */
    void delete(String sessionId);

    /**
     * 读取行程规划图状态（OverAllState 的 data Map）；不存在或出错返回 null。
     */
    java.util.Map<String, Object> loadGraphState(String sessionId);

    /**
     * 写回行程规划图状态并刷新 TTL（人机协作断点恢复）。
     */
    void saveGraphState(String sessionId, java.util.Map<String, Object> state);
}
