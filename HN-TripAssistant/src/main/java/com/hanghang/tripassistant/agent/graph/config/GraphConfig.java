package com.hanghang.tripassistant.agent.graph.config;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.hanghang.tripassistant.agent.graph.node.FinalizeAction;
import com.hanghang.tripassistant.agent.graph.node.HumanConfirmAction;
import com.hanghang.tripassistant.agent.graph.node.ParamCheckAction;
import com.hanghang.tripassistant.agent.graph.node.PoiEnrichAction;
import com.hanghang.tripassistant.agent.graph.node.ReplanAction;
import com.hanghang.tripassistant.agent.graph.node.SkeletonAction;
import com.hanghang.tripassistant.agent.graph.node.TransportAction;
import com.hanghang.tripassistant.agent.graph.state.TripPlanState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 行程规划图装配（设计文档 5.3 的编排 Agent）。
 * 节点间单向流转，条件边负责分流：
 * - paramCheck：缺参 → END（Handler 发追问）；齐全 → skeleton
 * - transport：新规划 → humanConfirm；调整 → replan（带旧骨架重排）
 * - humanConfirm：待确认 → END（软中断等用户）；已确认 → finalize 定稿
 * 人机协作 = 每轮 invoke 一次，图状态由 Handler 持久化到 Redis，下一轮整体重放（节点幂等）。
 */
@Configuration
public class GraphConfig {

    @Bean("tripPlanGraph")
    public CompiledGraph tripPlanGraph(ParamCheckAction paramCheck,
                                       SkeletonAction skeleton,
                                       PoiEnrichAction poiEnrich,
                                       TransportAction transport,
                                       ReplanAction replan,
                                       HumanConfirmAction humanConfirm,
                                       FinalizeAction finalize) throws GraphStateException {

        // 所有状态键统一 REPLACE 策略：节点返回什么就覆盖什么，语义直白
        KeyStrategyFactory keyStrategyFactory = () -> Map.of(
                TripPlanState.USER_INPUT, new ReplaceStrategy(),
                TripPlanState.SLOTS, new ReplaceStrategy(),
                TripPlanState.MODE, new ReplaceStrategy(),
                TripPlanState.SKELETON, new ReplaceStrategy(),
                TripPlanState.ENRICH, new ReplaceStrategy(),
                TripPlanState.TRANSPORT, new ReplaceStrategy(),
                TripPlanState.OUTPUT_TEXT, new ReplaceStrategy(),
                TripPlanState.ASK_MESSAGE, new ReplaceStrategy(),
                TripPlanState.NEED_CONFIRM, new ReplaceStrategy(),
                "tripPlan", new ReplaceStrategy());

        StateGraph stateGraph = new StateGraph(keyStrategyFactory)
                .addNode("paramCheck", node_async(paramCheck::apply))
                .addNode("skeleton", node_async(skeleton::apply))
                .addNode("poiEnrich", node_async(poiEnrich::apply))
                .addNode("transport", node_async(transport::apply))
                .addNode("replan", node_async(replan::apply))
                .addNode("humanConfirm", node_async(humanConfirm::apply))
                .addNode("finalize", node_async(finalize::apply))
                .addEdge(START, "paramCheck")
                // 参数不齐 → 直接结束（Handler 发追问）；齐全 → 生成骨架
                .addConditionalEdges("paramCheck",
                        state -> CompletableFuture.completedFuture(
                                StringUtils.hasText(state.value(TripPlanState.ASK_MESSAGE, "")) ? "ask" : "go"),
                        Map.of("ask", END, "go", "skeleton"))
                .addEdge("skeleton", "poiEnrich")
                .addEdge("poiEnrich", "transport")
                // 调整轮 → 先重排再确认；新规划 → 直接确认
                .addConditionalEdges("transport",
                        state -> CompletableFuture.completedFuture(
                                TripPlanState.MODE_ADJUST.equals(state.value(TripPlanState.MODE, "")) ? "adjust" : "new"),
                        Map.of("adjust", "replan", "new", "humanConfirm"))
                .addEdge("replan", "humanConfirm")
                // 待确认 → 结束等用户；已确认 → 定稿
                .addConditionalEdges("humanConfirm",
                        state -> CompletableFuture.completedFuture(
                                state.value(TripPlanState.NEED_CONFIRM, true) ? "wait" : "done"),
                        Map.of("wait", END, "done", "finalize"))
                .addEdge("finalize", END);

        return stateGraph.compile();
    }
}
