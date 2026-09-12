package com.hanghang.tripassistant.agent.handler.impl;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.hanghang.tripassistant.agent.context.SessionPhase;
import com.hanghang.tripassistant.agent.context.SessionStore;
import com.hanghang.tripassistant.agent.graph.state.TripPlanState;
import com.hanghang.tripassistant.agent.handler.IntentHandler;
import com.hanghang.tripassistant.agent.intent.IntentType;
import com.hanghang.tripassistant.agent.request.ChatContext;
import com.hanghang.tripassistant.agent.request.StreamEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 行程规划 Handler（编排 Agent 入口）：新建 / 调整行程的流式入口。
 * 内部委托给行程规划图（GraphConfig 装配）顺序编排（参数检查 → 骨架生成 → POI 校验 → 交通补全 → 人机确认 → 定稿），
 * 图状态持久化到 Redis（chat:graph:{sessionId}），每轮 invoke 一次、整体重放（节点幂等）。
 */
@Slf4j
@Component
public class TripPlanningHandler implements IntentHandler {

    @Autowired
    @Qualifier("tripPlanGraph")
    private CompiledGraph tripPlanGraph;

    @Autowired
    private SessionStore sessionStore;

    @Override
    public IntentType support() {
        return IntentType.TRIP_PLANNING;
    }

    @Override
    public Flux<StreamEvent> handleStream(ChatContext context) {
        // 图执行是同步多步编排，延迟到订阅时才跑（Flux.defer）
        return Flux.defer(() -> {
            try {
                OverAllState result = executeGraph(context);
                return buildEvents(context, result);
            } catch (Exception e) {
                log.error("[行程规划] 图执行失败 sessionId={}：{}", context.getSessionId(), e.getMessage());
                return Flux.just(StreamEvent.error("小岛民脑袋卡壳了，行程没排出来，稍后再试～"));
            }
        });
    }

    /** 构建图状态（含历史恢复）→ invoke → 结果写回 Redis */
    private OverAllState executeGraph(ChatContext context) {
        Map<String, Object> data = new HashMap<>();
        data.put(TripPlanState.USER_INPUT, context.getMessage());
        data.put(TripPlanState.SLOTS, context.getExtractParam() == null ? Map.of() : context.getExtractParam());

        // 历史图状态：上一轮的骨架 / 校验结果，用于调整轮整体重放
        Map<String, Object> saved = sessionStore.loadGraphState(context.getSessionId());
        boolean adjusting = context.getPhase() == SessionPhase.PLAN_DONE
                || (saved != null && saved.containsKey(TripPlanState.SKELETON));
        data.put(TripPlanState.MODE,
                adjusting ? TripPlanState.MODE_ADJUST : TripPlanState.MODE_NEW);
        if (saved != null && saved.get(TripPlanState.SKELETON) != null) {
            data.put(TripPlanState.SKELETON, saved.get(TripPlanState.SKELETON));
        } else if (context.getTripPlan() != null) {
            // 图状态丢失兜底：用业务快照里的旧行程
            data.put(TripPlanState.SKELETON, context.getTripPlan());
        }

        Optional<OverAllState> maybeResult = tripPlanGraph.invoke(data);
        OverAllState result = maybeResult.orElse(null);
        if (result == null) {
            throw new IllegalStateException("图执行无返回状态");
        }
        // 持久化图状态，下一轮调整/确认从断点语义恢复
        sessionStore.saveGraphState(context.getSessionId(), result.data());
        return result;
    }

    /** 图结果 → SSE 事件流：追问 / 草稿待确认 / 定稿 */
    private Flux<StreamEvent> buildEvents(ChatContext context, OverAllState result) {
        String askMessage = result.value(TripPlanState.ASK_MESSAGE, "");
        String outputText = result.value(TripPlanState.OUTPUT_TEXT, "");
        Object tripPlan = result.value("tripPlan", null);

        // 缺参追问：reply 用 outputText（可能有友好说明），ask 事件驱动前端继续输入
        if (StringUtils.hasText(askMessage)) {
            StreamEvent event = StreamEvent.ask(
                    StringUtils.hasText(outputText) ? outputText : askMessage, askMessage);
            return Flux.just(event);
        }

        List<StreamEvent> events = new ArrayList<>();
        // 文本按行切 token，模拟流式输出效果
        if (StringUtils.hasText(outputText)) {
            for (String line : outputText.split("\n", -1)) {
                events.add(StreamEvent.token(line + "\n"));
            }
        }
        // 定稿：end 事件带 tripPlan JSON，前端渲染行程卡
        if (tripPlan != null) {
            events.add(StreamEvent.end(tripPlan));
        }
        return Flux.fromIterable(events);
    }
}
