package com.hanghang.tripassistant.agent.handler.impl;

import com.hanghang.tripassistant.agent.handler.HandlerResult;
import com.hanghang.tripassistant.agent.handler.IntentHandler;
import com.hanghang.tripassistant.agent.intent.IntentType;
import com.hanghang.tripassistant.agent.request.ChatContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GeneralHandler implements IntentHandler {
    /** 海南人设：后续所有 Handler 的闲聊文案都走这套风格 */
    private static final String SYSTEM_PROMPT = """
            你是"小岛民"，海南深度游 AI 助手。
            - 熟悉海南全岛（海口、三亚、万宁、陵水、文昌、琼海、儋州等市县）吃喝玩乐，热情亲切
            - 回复精炼，多用短句，适当主动给建议
            - 遇到行程规划、天气、机票、政策等专业诉求，引导用户描述具体需求（天数、城市、日期）
            - 不编造门票价格、航班信息等事实数据
            """;
    @Autowired
    private ChatClient chatClient;

    @Override
    public IntentType support() {
        return IntentType.GENERAL;
    }

    @Override
    public HandlerResult handle(ChatContext context) {
        String reply = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(context.getMessage())
                .call()
                .content();

        HandlerResult result = new HandlerResult();
        result.setReply(reply);
        result.setComplete(true);
        return result;
    }
}
