package com.hanghang.tripassistant.agent.handler.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanghang.tripassistant.agent.handler.IntentHandler;
import com.hanghang.tripassistant.agent.intent.IntentType;
import com.hanghang.tripassistant.agent.request.ChatContext;
import com.hanghang.tripassistant.agent.request.StreamEvent;
import com.hanghang.tripassistant.service.GaoDeMapMcpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 交通查询 Handler：geo（地址→经纬度）→ 路径规划 两步工具链。
 * 槽位 from/to 优先（LLM 段提取），缺失时兜底取 cities 前两个城市；
 * 仍缺则逐项追问，追问态由 SessionManager 统一记忆。
 */
@Slf4j
@Component
public class TransportSearchHandler implements IntentHandler {

    /** 整理高德路径规划 JSON 的人设 prompt */
    private static final String FORMAT_PROMPT = """
            你是"小岛民"，海南深度游 AI 助手。
            下面是高德驾车路径规划接口返回的 JSON，请整理成简洁的出行方案：
            - 讲清起点、终点、总距离、预计用时
            - 简要描述路线走向（经过的主要路段/城市）
            - 语气热情亲切；JSON 里没有的数据不要编造
            """;

    @Autowired
    private GaoDeMapMcpService gaoDeMapMcpService;
    @Autowired
    private ChatClient chatClient;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public IntentType support() {
        return IntentType.TRANSPORT_SEARCH;
    }

    @Override
    public Flux<StreamEvent> handleStream(ChatContext context) {
        // defer：订阅时才执行同步的取槽位/调工具，避免请求线程提前阻塞
        return Flux.defer(() -> {
            Map<String, Object> slots = context.getIntentResult().getSlots();
            final String from = resolveFrom(slots);
            final String to = resolveTo(slots);

            if (!StringUtils.hasText(to)) {
                return Flux.just(StreamEvent.ask(
                        "想查交通的话，告诉小岛民要去哪里呀（比如三亚、海口）～",
                        "请问您的目的地是哪里？"));
            }
            if (!StringUtils.hasText(from)) {
                return Flux.just(StreamEvent.ask(
                        "好的，从哪里出发去" + to + "呢？（比如海口市区、机场）",
                        "请问您从哪里出发？"));
            }

            String rawJson;
            try {
                String origin = resolveLocation(from);
                String destination = resolveLocation(to);
                if (origin == null || destination == null) {
                    return Flux.just(StreamEvent.error(
                            "小岛民没在地图上找到这个位置，换个更具体的地名试试？（比如「海口美兰机场」）"));
                }
                rawJson = gaoDeMapMcpService.directionDriving(origin, destination);
            } catch (Exception e) {
                log.error("交通查询失败 from={} to={}：{}", from, to, e.getMessage());
                return Flux.just(StreamEvent.error("交通服务开小差了，稍后再试试～"));
            }

            return chatClient.prompt()
                    .system(FORMAT_PROMPT)
                    .user(rawJson)
                    .stream()
                    .content()
                    .map(StreamEvent::token)
                    .concatWith(Flux.just(StreamEvent.end(rawJson)))
                    .onErrorResume(e -> {
                        log.error("交通文案整理失败 from={} to={}：{}", from, to, e.getMessage());
                        return Flux.just(StreamEvent.error(
                                from + "到" + to + "的路线拿到了，但小岛民整理时走神了，稍后再问一次～"));
                    });
        });
    }

    /** 地址 → 经纬度 "lon,lat"；解析失败返回 null */
    private String resolveLocation(String address) {
        try {
            String geoJson = gaoDeMapMcpService.geo(address, null);
            JsonNode root = objectMapper.readTree(geoJson);
            JsonNode geocodes = root.path("geocodes");
            if (geocodes.isArray() && !geocodes.isEmpty()) {
                String location = geocodes.get(0).path("location").asText(null);
                if (StringUtils.hasText(location)) {
                    return location;
                }
            }
        } catch (Exception e) {
            log.error("地理编码失败 address={}：{}", address, e.getMessage());
        }
        return null;
    }

    /** 解析起点：from 槽位优先，缺失时兜底取第一个城市 */
    private String resolveFrom(Map<String, Object> slots) {
        String from = firstString(slots, "from");
        if (StringUtils.hasText(from)) {
            return from;
        }
        List<String> cities = cityList(slots);
        return cities.isEmpty() ? null : cities.get(0);
    }

    /** 解析终点：to 槽位优先，缺失时兜底取第二个城市 */
    private String resolveTo(Map<String, Object> slots) {
        String to = firstString(slots, "to");
        if (StringUtils.hasText(to)) {
            return to;
        }
        List<String> cities = cityList(slots);
        return cities.size() > 1 ? cities.get(1) : null;
    }

    /** 从槽位取字符串；没有则为 null */
    private String firstString(Map<String, Object> slots, String key) {
        if (slots == null || slots.get(key) == null) {
            return null;
        }
        return String.valueOf(slots.get(key));
    }

    /** 从槽位取城市列表；没有则为空列表 */
    private List<String> cityList(Map<String, Object> slots) {
        if (slots == null || !slots.containsKey("cities")) {
            return List.of();
        }
        Object cities = slots.get("cities");
        List<String> result = new ArrayList<>();
        if (cities instanceof List<?> list) {
            for (Object city : list) {
                result.add(String.valueOf(city));
            }
        }
        return result;
    }
}
