package com.hanghang.tripassistant.controller;

import cn.hutool.json.JSONUtil;
import com.hanghang.tripassistant.agent.request.ChatRequest;
import com.hanghang.tripassistant.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 统一聊天入口：/api/chat/send/stream（SSE 流式）。
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    @Autowired
    private ChatService chatService;

    /**
     * SSE 流式对话：事件为 StreamEvent JSON（meta/token/ask/error/end）。
     */
    @PostMapping(value = "/send/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sendStream(@RequestBody ChatRequest request) {
        // 事件流 → JSON 文本流：Spring 会自动补上 SSE 格式（data: 前缀）
        return chatService.chatStream(request)
                .map(JSONUtil::toJsonStr);
    }
}
