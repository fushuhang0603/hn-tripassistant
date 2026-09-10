package com.hanghang.tripassistant.agent.handler.impl;

import com.hanghang.tripassistant.agent.context.ChatMessage;
import com.hanghang.tripassistant.agent.context.MessageRole;
import com.hanghang.tripassistant.agent.handler.HandlerResult;
import com.hanghang.tripassistant.agent.handler.IntentHandler;
import com.hanghang.tripassistant.agent.intent.IntentType;
import com.hanghang.tripassistant.agent.request.ChatContext;
import com.hanghang.tripassistant.agent.request.StreamEvent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Component
public class GeneralHandler implements IntentHandler {
    /** 海南人设：后续所有 Handler 的闲聊文案都走这套风格 */
    private static final String SYSTEM_PROMPT = """
            你叫"小岛民"，海南深度游 AI 助手。
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
                .user(buildUserPrompt(context))
                .call()
                .content();

        HandlerResult result = new HandlerResult();
        result.setReply(reply);
        result.setComplete(true);
        return result;
    }

    @Override
    public Flux<StreamEvent> handleStream(ChatContext context) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(buildUserPrompt(context))
                .stream()
                .content()
                .map(StreamEvent::token);
    }

    /**
     * 组装用户侧 prompt：对话历史 + 已收集槽位 + 本轮消息，让 LLM 具备会话记忆。
     */
    private String buildUserPrompt(ChatContext context) {
        StringBuilder sb = new StringBuilder();
        List<ChatMessage> history = context.getHistory();
        if (history != null && !history.isEmpty()) {
            sb.append("【对话历史（按时间顺序）】\n");
            for (ChatMessage msg : history) {
                sb.append(msg.getRole() == MessageRole.USER ? "用户: " : "小岛民: ")
                        .append(msg.getContent()).append("\n");
            }
            sb.append("\n");
        }
        Map<String, Object> slots = context.getExtractParam();
        if (slots != null && !slots.isEmpty()) {
            sb.append("【当前已收集的信息】").append(slots).append("\n\n");
        }
        sb.append("【用户最新消息】").append(context.getMessage());
        return sb.toString();
    }
}
