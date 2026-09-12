package com.hanghang.tripassistant.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.hanghang.tripassistant.agent.graph.state.TripPlanState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 节点1：参数完整性检查（纯规则，0 token）。
 * 缺城市 → 问城市；缺天数 → 问天数；齐全 → 放行生成。
 * 一次只追问一项，让用户回答压力最小。
 */
@Component
public class ParamCheckAction implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        Map<String, Object> slots = state.value(TripPlanState.SLOTS, Map.of());
        List<?> cities = (List<?>) slots.get("cities");
        Object days = slots.get("days");
        if (cities == null || cities.isEmpty()) {
            return Map.of(
                    TripPlanState.ASK_MESSAGE, "想去海南哪个城市玩呀？（比如：三亚）");
        }
        if (days == null || days.toString().isBlank()) {
            return Map.of(
                    TripPlanState.ASK_MESSAGE, "打算玩几天呀？（比如：3天）");
        }
        return Map.of();
    }
}
