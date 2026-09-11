package com.hanghang.tripassistant.service;

import com.hanghang.tripassistant.agent.request.ChatRequest;
import com.hanghang.tripassistant.agent.request.StreamEvent;
import reactor.core.publisher.Flux;

public interface ChatService {

    /**
     * 流式对话（SSE）：首事件 meta（sessionId/意图），随后 token 增量，
     * 终态事件 ask/error/end。
     */
    Flux<StreamEvent> chatStream(ChatRequest request);
}
