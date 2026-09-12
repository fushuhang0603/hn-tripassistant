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

import java.util.Map;

/**
 * 节点2：行程骨架生成（LLM）。
 * 按槽位（城市/天数/日期）生成 JSON 骨架：
 * {"days":[{"day":1,"city":"三亚","theme":"海滨初见","items":[{"time":"上午","place":"亚龙湾","note":"..."}]}]}
 * 调整模式（ADJUST）下透传旧骨架，由 replan 节点负责重排，避免重复生成。
 */
@Slf4j
@Component
public class SkeletonAction implements NodeAction {

    /** 骨架生成人设：海南知识 + 严格 JSON 输出 */
    private static final String SYSTEM_PROMPT = """
            你是"小岛民"，海南深度游 AI 行程规划师。
            请根据用户的出行信息生成行程骨架，严格按照以下 JSON 结构输出，不要输出任何多余文字、解释或 Markdown：
            {"days":[{"day":1,"city":"城市名","theme":"当天主题","items":[{"time":"上午|中午|下午|晚上","place":"具体地点","note":"一句话推荐理由"}]}]}
            要求：
            - 每天 3-4 个地点，地点必须真实存在，行程节奏合理（考虑车程与体力）
            - 跨城市行程按"先远后近或动线顺路"安排
            - note 精炼、口语化，有小岛民风格
            """;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        // 调整模式：骨架已存在，直接透传（幂等），重排交给 replan
        if (TripPlanState.MODE_ADJUST.equals(state.value(TripPlanState.MODE, ""))) {
            return Map.of();
        }
        Map<String, Object> slots = state.value(TripPlanState.SLOTS, Map.of());
        String userPrompt = "用户出行信息：" + slots + "\n请生成行程骨架 JSON。";
        String raw = callLlm(userPrompt, null);
        JsonNode skeleton = parseSkeleton(raw);
        if (skeleton == null) {
            // 解析失败重试一次，附上错误提示
            raw = callLlm(userPrompt + "\n上次输出无法解析为 JSON，请严格输出纯 JSON。", null);
            skeleton = parseSkeleton(raw);
        }
        if (skeleton == null) {
            throw new IllegalStateException("行程骨架生成失败：LLM 输出无法解析为 JSON");
        }
        return Map.of(TripPlanState.SKELETON, skeleton);
    }

    /** 非流式调用 LLM（图节点内同步收集完整输出） */
    private String callLlm(String userPrompt, String fallback) {
        try {
            String content = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();
            return StringUtils.hasText(content) ? content : fallback;
        } catch (Exception e) {
            log.error("[行程规划] 骨架生成 LLM 调用失败：{}", e.getMessage());
            return fallback;
        }
    }

    /** 解析 LLM 输出为骨架 JSON；失败返回 null */
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
            log.warn("[行程规划] 骨架 JSON 解析失败：{}", e.getMessage());
        }
        return null;
    }
}
