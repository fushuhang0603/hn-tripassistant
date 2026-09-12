package com.hanghang.tripassistant.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanghang.tripassistant.agent.graph.state.TripPlanState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 节点5：调整指令处理（确认 / 修改 / 闲聊三分流）。
 * - 确认词（可以/没问题/就这样…）→ 透传骨架，needConfirm=false，进入定稿
 * - 其他 → 视为修改指令，带旧骨架 + 指令让 LLM 重排，needConfirm=true 再确认
 * prompt 中约束：闲聊或无关内容保持原行程不变。
 */
@Slf4j
@Component
public class ReplanAction implements NodeAction {

    /** 确认词表：命中即定稿 */
    private static final List<String> CONFIRM_WORDS = List.of(
            "可以", "没问题", "就这样", "就这个", "定了", "定稿", "ok", "好的", "行", "不错", "挺好", "满意");

    /** 重排人设：保持 JSON 结构一致 */
    private static final String SYSTEM_PROMPT = """
            你是"小岛民"，海南深度游 AI 行程规划师。
            用户对已生成的行程提出了调整要求，请修改行程骨架后重新输出，严格按照以下 JSON 结构输出，不要输出任何多余文字、解释或 Markdown：
            {"days":[{"day":1,"city":"城市名","theme":"当天主题","items":[{"time":"上午|中午|下午|晚上","place":"具体地点","note":"一句话推荐理由"}]}]}
            要求：
            - 只调整用户要求的部分，其余天数尽量保持不变
            - 地点必须真实存在，行程节奏合理
            - 如果用户的消息与行程无关（闲聊），原样返回旧行程
            """;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String userInput = state.value(TripPlanState.USER_INPUT, "");
        JsonNode oldSkeleton = skeleton(state);

        // 1. 确认词 → 定稿
        if (isConfirm(userInput)) {
            return Map.of(TripPlanState.NEED_CONFIRM, false);
        }

        // 2. 修改指令 → LLM 重排
        String userPrompt = "旧行程骨架：\n" + (oldSkeleton == null ? "（无）" : oldSkeleton.toPrettyString())
                + "\n\n用户调整要求：" + userInput
                + "\n请输出调整后的行程骨架 JSON。";
        String raw = callLlm(userPrompt);
        JsonNode newSkeleton = parseSkeleton(raw);
        if (newSkeleton == null) {
            // 重排失败：保留旧骨架，继续等待用户（不阻塞会话）
            log.warn("[行程规划] 重排解析失败，保留旧骨架");
            return Map.of(TripPlanState.NEED_CONFIRM, true);
        }
        return Map.of(
                TripPlanState.SKELETON, newSkeleton,
                TripPlanState.NEED_CONFIRM, true);
    }

    private boolean isConfirm(String message) {
        if (!StringUtils.hasText(message)) {
            return false;
        }
        String lower = message.trim().toLowerCase();
        return CONFIRM_WORDS.stream().anyMatch(lower::contains);
    }

    private String callLlm(String userPrompt) {
        try {
            String content = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();
            return StringUtils.hasText(content) ? content : null;
        } catch (Exception e) {
            log.error("[行程规划] 重排 LLM 调用失败：{}", e.getMessage());
            return null;
        }
    }

    private JsonNode parseSkeleton(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(raw);
            if (node.has("days") && node.path("days").isArray()) {
                return node;
            }
        } catch (Exception e) {
            log.warn("[行程规划] 重排 JSON 解析失败：{}", e.getMessage());
        }
        return null;
    }

    private JsonNode skeleton(OverAllState state) {
        Object value = state.value(TripPlanState.SKELETON, null);
        if (value == null) {
            return null;
        }
        return value instanceof JsonNode node ? node : objectMapper.valueToTree(value);
    }
}
