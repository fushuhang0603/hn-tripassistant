package com.hanghang.tripassistant.agent.recognizer;

import com.hanghang.tripassistant.agent.intent.IntentResult;
import com.hanghang.tripassistant.agent.request.ChatContext;

/**
 * 意图识别器：统一识别入口。
 * 内部两段：规则段（高置信短路，0 token）→ LLM 段（结构化分类）；
 * 低置信度（< 0.5）由实现强制降级 GENERAL。
 */
public interface IntentRecognizer {

    /**
     * 识别意图
     *
     * @param message 用户本轮原始消息
     * @param context 会话上下文（当前仅用户信息，SessionManager 接入后注入 phase/extractParam）
     * @return 识别结果，intent 恒非空（识别失败兜底 GENERAL）
     */
    IntentResult recognize(String message, ChatContext context);
}
