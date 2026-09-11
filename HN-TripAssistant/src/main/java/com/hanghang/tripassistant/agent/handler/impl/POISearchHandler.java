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
 * 景点/美食查询 Handler：三步模式（取槽位 → 调工具 → LLM 整理）。
 * 缺关键词或城市时追问，追问态由 SessionManager 统一记忆。
 */
@Slf4j
@Component
public class POISearchHandler implements IntentHandler {

    /** 整理高德 POI 搜索 JSON 的人设 prompt */
    private static final String FORMAT_PROMPT = """
            你是"小岛民"，海南深度游 AI 助手。
            下面是高德 POI 搜索接口返回的 JSON，请整理成简洁的推荐清单：
            - 挑出与用户需求最相关的 3-5 个地点，讲清名称、位置、特色
            - 语气热情亲切，像本地人安利
            - JSON 里没有的数据不要编造（价格、评分没有就不提）
            """;

    @Autowired
    private GaoDeMapMcpService gaoDeMapMcpService;
    @Autowired
    private ChatClient chatClient;

    @Override
    public IntentType support() {
        return IntentType.POI_SEARCH;
    }

    @Override
    public Flux<StreamEvent> handleStream(ChatContext context) {
        // defer：订阅时才执行同步的取槽位/调工具，避免请求线程提前阻塞
        return Flux.defer(() -> {
            Map<String, Object> slots = context.getIntentResult().getSlots();
            String keyword = firstKeyword(slots);
            String city = firstCity(slots);
            if (!StringUtils.hasText(keyword)) {
                return Flux.just(StreamEvent.ask(
                        "想找点啥呢？告诉小岛民类型或名字呀（比如海鲜、夜市、天涯海角）～",
                        "请问您想找什么景点或美食？"));
            }
            if (!StringUtils.hasText(city)) {
                return Flux.just(StreamEvent.ask(
                        "好的，在哪个城市找" + keyword + "呢？（比如海口、三亚）",
                        "请问在哪个城市？"));
            }

            String rawJson;
            try {
                rawJson = gaoDeMapMcpService.textSearch(keyword, city);
            } catch (Exception e) {
                log.error("POI 搜索失败 keyword={} city={}：{}", keyword, city, e.getMessage());
                return Flux.just(StreamEvent.error("搜索服务开小差了，稍后再试试～"));
            }

            return chatClient.prompt()
                    .system(FORMAT_PROMPT)
                    .user(rawJson)
                    .stream()
                    .content()
                    .map(StreamEvent::token)
                    .concatWith(Flux.just(StreamEvent.end(rawJson)))
                    .onErrorResume(e -> {
                        log.error("POI 文案整理失败 keyword={} city={}：{}", keyword, city, e.getMessage());
                        return Flux.just(StreamEvent.error(
                                keyword + "的搜索结果拿到了，但小岛民整理时走神了，稍后再问一次～"));
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
