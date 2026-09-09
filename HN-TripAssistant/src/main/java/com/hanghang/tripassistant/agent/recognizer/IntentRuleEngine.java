package com.hanghang.tripassistant.agent.recognizer;

import com.hanghang.tripassistant.agent.intent.IntentResult;
import com.hanghang.tripassistant.agent.intent.IntentType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则段：高频确定性意图的纯文本匹配，命中即短路（0 token、毫秒级）。
 * 当前规则量小，采用「关键词包含 + 正则」顺序匹配；规则膨胀后可替换为 AC 多模式匹配。
 */
@Component
public class IntentRuleEngine {

    /** 海南市县列表，用于 cities 槽位提取（"东方"用全名"东方市"，避免误伤"东方航空"） */
    private static final List<String> HAINAN_CITIES = List.of(
            "海口", "三亚", "万宁", "陵水", "文昌", "琼海", "儋州",
            "五指山", "东方市", "澄迈", "临高", "屯昌", "定安", "乐东", "保亭", "白沙", "昌江", "琼中");

    /** days 槽位：3天 / 玩3天（不含"日"，避免把日期"10月1日"误提取为 1 天） */
    private static final Pattern DAYS_PATTERN = Pattern.compile("(\\d+)\\s*天");

    /** travelDate 槽位：2026-10-01 / 2026/10/01 / 2026年10月1日 */
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}日?)");

    /**
     * 规则表：顺序即优先级，先命中者胜出。
     * 关键词任一包含 或 正则任一命中 即算命中，置信度均 ≥ 0.9 满足短路条件。
     */
    private static final List<IntentRule> RULES = List.of(
            new IntentRule(IntentType.TRIP_PLANNING, 0.95,
                    List.of("规划", "安排", "行程", "路线", "计划", "几天"),
                    List.of(Pattern.compile("玩.*天"))),
            new IntentRule(IntentType.TRANSPORT_SEARCH, 0.95,
                    List.of("机票", "航班", "高铁", "火车", "轮船", "怎么去", "交通", "飞往"),
                    List.of()),
            new IntentRule(IntentType.HOTEL_SEARCH, 0.95,
                    List.of("酒店", "民宿", "住宿", "宾馆", "住哪", "订房"),
                    List.of()),
            new IntentRule(IntentType.WEATHER, 0.95,
                    List.of("天气", "台风", "下雨", "气温", "温度"),
                    List.of()),
            new IntentRule(IntentType.POLICY_QA, 0.95,
                    List.of("免税", "离岛", "轮渡", "登岛", "政策", "限购", "海关"),
                    List.of()),
            new IntentRule(IntentType.RAG_QA, 0.92,
                    List.of("攻略", "必去", "必玩", "怎么玩"),
                    List.of()),
            new IntentRule(IntentType.POI_SEARCH, 0.92,
                    List.of("景点", "美食", "海鲜", "好吃", "好玩", "网红", "打卡", "夜市"),
                    List.of()),
            new IntentRule(IntentType.GENERAL, 0.9,
                    List.of("算了", "不用了", "取消"),
                    List.of()));

    /**
     * 规则匹配：未命中返回 null，由调用方进入 LLM 段。
     */
    public IntentResult match(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        for (IntentRule rule : RULES) {
            boolean hit = rule.keywords().stream().anyMatch(message::contains)
                    || rule.patterns().stream().anyMatch(p -> p.matcher(message).find());
            if (hit) {
                return buildResult(rule, message);
            }
        }
        return null;
    }

    private IntentResult buildResult(IntentRule rule, String message) {
        IntentResult result = new IntentResult();
        result.setIntent(rule.intent());
        result.setConfidence(rule.confidence());
        result.setSource(IntentResult.Source.RULE);
        result.setSlots(extractSlots(message));
        return result;
    }

    /**
     * 通用槽位提取：days / cities / travelDate，提取到才写入，未提取到不占 key。
     * from/to 等复杂槽位规则段不处理，交给 LLM 段提取。
     */
    private Map<String, Object> extractSlots(String message) {
        Map<String, Object> slots = new HashMap<>();

        Matcher daysMatcher = DAYS_PATTERN.matcher(message);
        if (daysMatcher.find()) {
            slots.put("days", daysMatcher.group(1));
        }

        List<String> cities = HAINAN_CITIES.stream().filter(message::contains).toList();
        if (!cities.isEmpty()) {
            slots.put("cities", cities);
        }

        Matcher dateMatcher = DATE_PATTERN.matcher(message);
        if (dateMatcher.find()) {
            slots.put("travelDate", normalizeDate(dateMatcher.group(1)));
        }
        return slots;
    }

    /** 日期归一化：2026年10月1日 / 2026/10/01 → 2026-10-1 */
    private String normalizeDate(String raw) {
        return raw.replace("年", "-").replace("月", "-").replace("/", "-").replace("日", "");
    }

    /** 一条规则：意图 + 命中置信度 + 关键词组 + 正则组 */
    private record IntentRule(IntentType intent, double confidence, List<String> keywords,
                              List<Pattern> patterns) {
    }
}
