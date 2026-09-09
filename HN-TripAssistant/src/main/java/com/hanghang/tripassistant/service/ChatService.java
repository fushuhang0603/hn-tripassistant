package com.hanghang.tripassistant.service;

import com.hanghang.tripassistant.agent.request.ChatRequest;
import com.hanghang.tripassistant.agent.request.ChatResponse;

public interface ChatService {

    public ChatResponse chat(ChatRequest request);
}
