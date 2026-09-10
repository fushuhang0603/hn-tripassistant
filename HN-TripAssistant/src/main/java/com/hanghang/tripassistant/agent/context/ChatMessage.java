package com.hanghang.tripassistant.agent.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 一条对话消息：会话历史的最小单元，存 Redis chat:history:{sessionId} List。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /** 发言方 */
    private MessageRole role;

    /** 消息内容（短文本；大 JSON 不进历史，只存回复文案） */
    private String content;

    /** 产生时间 */
    private LocalDateTime time;

    /**
     * 工厂方法
     * @param role
     * @param content
     * @return
     */
     public static ChatMessage of(MessageRole role, String content) {
        return new ChatMessage(role, content, LocalDateTime.now());
    }
}
