package com.hanghang.tripassistant.controller;

import cn.hutool.json.JSONUtil;
import com.hanghang.tripassistant.agent.request.ChatRequest;
import com.hanghang.tripassistant.agent.request.ChatResponse;
import com.hanghang.tripassistant.agent.request.StreamEvent;
import com.hanghang.tripassistant.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 统一聊天入口：/api/chat/send
 * 所有对话请求唯一入口；/send/stream 为 SSE 流式版本。
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    @Autowired
    private ChatService chatService;

    @PostMapping("/send")
    public ChatResponse send(@RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    /**
     * SSE 流式对话：事件为 StreamEvent JSON（meta/token/ask/error/end）。
     */
    @PostMapping(value = "/send/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> sendStream(@RequestBody ChatRequest request) {
        return chatService.chatStream(request)
                .map(event -> ServerSentEvent.builder(JSONUtil.toJsonStr(event)).build());
    }
}
