package com.hanghang.tripassistant.agent.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis 会话存储：快照 String(JSON) + 历史 List，TTL 24h，活跃会话读写刷新。
 * Key 设计见设计文档 6.2；序列化用 Jackson，Redis 侧可直接 CLI 排查。
 */
@Slf4j
@Component
public class RedisSessionStore implements SessionStore {

    /** 快照 key 前缀：chat:session:{sessionId} */
    private static final String SESSION_KEY_PREFIX = "chat:session:";
    /** 历史 key 前缀：chat:history:{sessionId} */
    private static final String HISTORY_KEY_PREFIX = "chat:history:";
    /** 会话数据存活时长：24h，每次读写刷新 */
    private static final Duration TTL = Duration.ofHours(24);
    /** 消息历史保留条数上限 */
    private static final int MAX_HISTORY = 30;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisSessionStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatSession load(String sessionId) {
        String json = redisTemplate.opsForValue().get(sessionKey(sessionId));
        if (json == null) {
            return null;
        }
        try {
            ChatSession session = objectMapper.readValue(json, ChatSession.class);
            redisTemplate.expire(sessionKey(sessionId), TTL);
            return session;
        } catch (JsonProcessingException e) {
            log.error("[会话] 快照反序列化失败 sessionId={}：{}", sessionId, e.getMessage());
            return null;
        }
    }

    @Override
    public void save(ChatSession session) {
        try {
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(sessionKey(session.getSessionId()), json, TTL);
        } catch (JsonProcessingException e) {
            log.error("[会话] 快照序列化失败 sessionId={}：{}", session.getSessionId(), e.getMessage());
        }
    }

    @Override
    public void appendMessage(String sessionId, ChatMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            String key = historyKey(sessionId);
            redisTemplate.opsForList().leftPush(key, json);
            redisTemplate.opsForList().trim(key, 0, MAX_HISTORY - 1);
            redisTemplate.expire(key, TTL);
        } catch (JsonProcessingException e) {
            log.error("[会话] 消息序列化失败 sessionId={}：{}", sessionId, e.getMessage());
        }
    }

    @Override
    public List<ChatMessage> recentMessages(String sessionId, int n) {
        List<String> jsons = redisTemplate.opsForList().range(historyKey(sessionId), 0, n - 1);
        if (jsons == null || jsons.isEmpty()) {
            return List.of();
        }
        // LPUSH 最新在前，倒序遍历还原时间正序
        List<ChatMessage> messages = new ArrayList<>(jsons.size());
        for (int i = jsons.size() - 1; i >= 0; i--) {
            try {
                messages.add(objectMapper.readValue(jsons.get(i), ChatMessage.class));
            } catch (JsonProcessingException e) {
                log.error("[会话] 消息反序列化失败 sessionId={}：{}", sessionId, e.getMessage());
            }
        }
        return messages;
    }

    @Override
    public void delete(String sessionId) {
        redisTemplate.delete(List.of(sessionKey(sessionId), historyKey(sessionId)));
    }

    private String sessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }

    private String historyKey(String sessionId) {
        return HISTORY_KEY_PREFIX + sessionId;
    }
}
