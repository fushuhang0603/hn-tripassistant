package com.hanghang.tripassistant.agent.handler.impl;

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

import java.util.List;
import java.util.Map;

/**
 * 住宿查询 Handler：三步模式（取槽位 → 调工具 → LLM 整理）。
 * 城市必填；关键词（如海景/亲子）可选，缺省按"酒店"搜索。
 */
@Slf4j
@Component
public class HotelSearchHandler implements IntentHandler {

    /** 整理高德住宿搜索 JSON 的人设 prompt */
    private static final String FORMAT_PROMPT = """
            你是"小岛民"，海南深度游 AI 助手。
            下面是高德关键词搜索返回的住宿类 JSON，请整理成简洁的推荐清单：
            - 挑出 3-5 个合适的住宿，讲清名称、位置、特色
            - 结合用户补充的关键词（如海景、亲子、平价）做针对性推荐
            - 语气热情亲切；JSON 里没有的数据不要编造（价格、星级没有就不提）
            """;

    @Autowired
    private GaoDeMapMcpService gaoDeMapMcpService;
    @Autowired
    private ChatClient chatClient;

    @Override
    public IntentType support() {
        return IntentType.HOTEL_SEARCH;
    }

    @Override
    public Flux<StreamEvent> handleStream(ChatContext context) {
        // defer：订阅时才执行同步的取槽位/调工具，避免请求线程提前阻塞
        return Flux.defer(() -> {
            Map<String, Object> slots = context.getIntentResult().getSlots();
            String city = firstCity(slots);
            if (!StringUtils.hasText(city)) {
                return Flux.just(StreamEvent.ask(
                        "想订住宿的话，告诉小岛民在哪个城市呀（比如三亚、海口）～",
                        "请问您想在哪个城市找住宿？"));
            }
            String keyword = firstKeyword(slots);
            String searchKeyword = StringUtils.hasText(keyword) ? keyword + " 住宿" : "酒店";

            String rawJson;
            try {
                rawJson = gaoDeMapMcpService.textSearch(searchKeyword, city);
            } catch (Exception e) {
                log.error("住宿搜索失败 keyword={} city={}：{}", searchKeyword, city, e.getMessage());
                return Flux.just(StreamEvent.error("住宿服务开小差了，稍后再试试～"));
            }

            return chatClient.prompt()
                    .system(FORMAT_PROMPT)
                    .user(rawJson)
                    .stream()
                    .content()
                    .map(StreamEvent::token)
                    .concatWith(Flux.just(StreamEvent.end(rawJson)))
                    .onErrorResume(e -> {
                        log.error("住宿文案整理失败 keyword={} city={}：{}", searchKeyword, city, e.getMessage());
                        return Flux.just(StreamEvent.error(
                                city + "的住宿数据拿到了，但小岛民整理时走神了，稍后再问一次～"));
                    });
        });
    }

    /** 从槽位取关键词；没有则为 null */
    private String firstKeyword(Map<String, Object> slots) {
        if (slots == null || slots.get("keyword") == null) {
            return null;
        }
        return String.valueOf(slots.get("keyword"));
    }

    /** 从槽位取第一个城市；没有则为 null */
    private String firstCity(Map<String, Object> slots) {
        if (slots == null || !slots.containsKey("cities")) {
            return null;
        }
        Object cities = slots.get("cities");
        if (cities instanceof List<?> list && !list.isEmpty()) {
            return String.valueOf(list.get(0));
        }
        return null;
    }
}
