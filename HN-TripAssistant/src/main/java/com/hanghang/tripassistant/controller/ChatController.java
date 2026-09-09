package com.hanghang.tripassistant.controller;

import com.hanghang.tripassistant.agent.request.ChatRequest;
import com.hanghang.tripassistant.agent.request.ChatResponse;
import com.hanghang.tripassistant.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 统一聊天入口：/api/chat/send
 * 所有对话请求唯一入口
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
}
