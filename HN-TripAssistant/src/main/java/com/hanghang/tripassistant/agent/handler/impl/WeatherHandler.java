package com.hanghang.tripassistant.agent.handler.impl;

import com.hanghang.tripassistant.agent.handler.HandlerResult;
import com.hanghang.tripassistant.agent.handler.IntentHandler;
import com.hanghang.tripassistant.agent.intent.IntentType;
import com.hanghang.tripassistant.agent.request.ChatContext;
import com.hanghang.tripassistant.agent.request.StreamEvent;
import com.hanghang.tripassistant.service.GaoDeMapMcpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 天气查询 Handler：三步模式样板（取槽位 → 调工具 → LLM 整理）。
 * 后续 POI / Hotel / Transport 等查询类 Handler 照此结构复制。
 */
@Slf4j
@Component
public class WeatherHandler implements IntentHandler {

    /** 整理高德天气 JSON 的人设 prompt */
    private static final String FORMAT_PROMPT = """
            你是"小岛民"，海南深度游 AI 助手。
            下面是高德天气接口返回的 JSON，请整理成简洁口语化的天气播报：
            - 讲清城市、日期、天气现象、气温、风力、湿度
            - 适当给穿衣/出行小建议，语气热情亲切
            - JSON 里没有的数据不要编造
            """;

    @Autowired
    private GaoDeMapMcpService gaoDeMapMcpService;
    @Autowired
    private ChatClient chatClient;

    @Override
    public IntentType support() {
        return IntentType.WEATHER;
    }

    @Override
    public HandlerResult handle(ChatContext context) {
        HandlerResult result = new HandlerResult();

        //取槽位：城市（识别器规则段/LLM 段都会提取 cities）
        String city = firstCity(context.getIntentResult().getSlots());
        if (city == null) {
            // 缺参追问，不调工具
            result.setReply("想查天气的话，告诉小岛民是哪个城市呀（比如海口、三亚）～");
            result.setAskMessage("请问您想查哪个城市的天气？");
            result.setComplete(true);
            return result;
        }

        //调工具：高德天气，返回原始 JSON
        String rawJson;
        try {
            rawJson = gaoDeMapMcpService.weather(city);
        } catch (Exception e) {
            log.error("天气查询失败 city={}：{}", city, e.getMessage());
            result.setReply("天气服务开小差了，稍后再试试～");
            result.setComplete(true);
            return result;
        }

        //LLM 整理成；整理失败回退固定文案，不让用户看到 JSON
        String reply;
        try {
            reply = chatClient.prompt()
                    .system(FORMAT_PROMPT)
                    .user(rawJson)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("天气文案整理失败 city={}：{}", city, e.getMessage());
            reply = city + "的天气数据拿到了，但小岛民整理时走神了，稍后再问一次～";
        }

        result.setReply(reply);
        result.setComplete(true);
        result.setData(rawJson);
        return result;
    }

    @Override
    public Flux<StreamEvent> handleStream(ChatContext context) {
        // defer：订阅时才执行同步的取槽位/调工具，避免请求线程提前阻塞
        return Flux.defer(() -> {
            // 取槽位：城市（识别器规则段/LLM 段都会提取 cities）
            String city = firstCity(context.getIntentResult().getSlots());
            if (city == null) {
                return Flux.just(StreamEvent.ask(
                        "想查天气的话，告诉小岛民是哪个城市呀（比如海口、三亚）～",
                        "请问您想查哪个城市的天气？"));
            }

            // 调工具：高德天气，返回原始 JSON
            String rawJson;
            try {
                rawJson = gaoDeMapMcpService.weather(city);
            } catch (Exception e) {
                log.error("天气查询失败 city={}：{}", city, e.getMessage());
                return Flux.just(StreamEvent.error("天气服务开小差了，稍后再试试～"));
            }

            // LLM 流式整理；整理失败回退固定文案，不让用户看到 JSON
            return chatClient.prompt()
                    .system(FORMAT_PROMPT)
                    .user(rawJson)
                    .stream()
                    .content()
                    .map(StreamEvent::token)
                    .concatWith(Flux.just(StreamEvent.end(rawJson)))
                    .onErrorResume(e -> {
                        log.error("天气文案整理失败 city={}：{}", city, e.getMessage());
                        return Flux.just(StreamEvent.error(
                                city + "的天气数据拿到了，但小岛民整理时走神了，稍后再问一次～"));
                    });
        });
    }

    /** 从识别槽位里取第一个城市；没有则为 null */
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
