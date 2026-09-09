package com.hanghang.tripassistant.agent.recognizer;

import com.hanghang.tripassistant.agent.intent.IntentResult;
import com.hanghang.tripassistant.agent.intent.IntentType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * LLM 段：规则段未命中时，一次 LLM 调用输出结构化分类结果。
 * 与 ExtractParamNode 同套路（BeanOutputConverter 强制 JSON 输出）。
 * 模型不可用或输出非法时兜底降级 GENERAL，不向上抛异常。
 */
@Slf4j
@Component
public class LLMIntentClassifier {

    private static final String SYSTEM_PROMPT = """
            你是"小岛民"海南深度游 AI 助手的意图分类器，请把用户输入分类到以下意图（严格使用枚举名）：
            - TRIP_PLANNING：行程规划/调整，如"三亚玩3天怎么安排"
            - TRANSPORT_SEARCH：交通方式，如机票、航班、高铁、怎么去某地
            - HOTEL_SEARCH：住宿，如酒店、民宿
            - POI_SEARCH：具体景点/美食/游玩地点查询
            - WEATHER：天气、台风、气温
            - POLICY_QA：政策问答，如免税、离岛轮渡、登岛规定
            - RAG_QA：求攻略、求推荐清单，如"海口有什么好玩的"
            - GENERAL：闲聊、问候、与旅游无关的话题

            判定要点：
            1. 含行程生成/调整语义的优先 TRIP_PLANNING；
            2. 求攻略/推荐清单归 RAG_QA，问具体某个点在哪归 POI_SEARCH；
            3. 拿不准或闲聊归 GENERAL，confidence 给低分。

            slots 只提取消息中明确出现的槽位，没有的不要编造，值为 null：
            - days：旅行天数，数字字符串，如"3"
            - cities：海南城市，数组，如["三亚"]
            - travelDate：日期，格式 YYYY-MM-DD
            - keyword：查询核心词

            只输出 JSON，不要任何解释或 Markdown：
            """;

    private final ChatClient chatClient;

    public LLMIntentClassifier(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * LLM 分类；调用失败（模型不可用/输出非法）时兜底降级 GENERAL。
     */
    public IntentResult classify(String message) {
        BeanOutputConverter<ClassificationResult> converter =
                new BeanOutputConverter<>(new ParameterizedTypeReference<>() {
                });
        try {
            String content = chatClient.prompt()
                    .system(s -> s.text(SYSTEM_PROMPT + converter.getFormat()))
                    .user(message)
                    .call()
                    .content();
            return toIntentResult(converter.convert(content));
        } catch (Exception e) {
            log.error("LLM 意图分类失败，降级 GENERAL：{}", e.getMessage());
            IntentResult result = new IntentResult();
            result.setIntent(IntentType.GENERAL);
            result.setConfidence(0.0);
            result.setSource(IntentResult.Source.LLM);
            return result;
        }
    }

    private IntentResult toIntentResult(ClassificationResult raw) {
        IntentResult result = new IntentResult();
        result.setIntent(parseIntent(raw.getIntent()));
        result.setConfidence(raw.getConfidence() == null ? 0.5 : raw.getConfidence());
        result.setSlots(raw.getSlots());
        result.setSource(IntentResult.Source.LLM);
        return result;
    }

    /** 容错解析：大小写/多余描述均可，含枚举名即匹配；解析不了降级 GENERAL */
    private IntentType parseIntent(String name) {
        if (name == null || name.isBlank()) {
            return IntentType.GENERAL;
        }
        String upper = name.trim().toUpperCase();
        for (IntentType type : IntentType.values()) {
            if (upper.contains(type.name())) {
                return type;
            }
        }
        log.warn("LLM 返回未知意图 [{}]，降级 GENERAL", name);
        return IntentType.GENERAL;
    }

    /** LLM 结构化输出目标：与 prompt 中说明的 JSON 结构一致 */
    @Data
    public static class ClassificationResult {
        private String intent;
        private Double confidence;
        private Map<String, Object> slots;
    }
}
