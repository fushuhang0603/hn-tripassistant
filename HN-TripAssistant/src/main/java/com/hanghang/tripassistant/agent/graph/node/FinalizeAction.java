package com.hanghang.tripassistant.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanghang.tripassistant.agent.graph.state.TripPlanState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 节点7：定稿收口。
 * 用户确认后把最终骨架作为 tripPlan 写回图状态，
 * Handler 据此发 end 事件（tripPlan JSON 给前端渲染行程卡）并落业务快照。
 */
@Component
public class FinalizeAction implements NodeAction {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        JsonNode skeleton = skeleton(state);
        return Map.of("tripPlan", skeleton == null ? Map.of() : skeleton);
    }

    private JsonNode skeleton(OverAllState state) {
        Object value = state.value(TripPlanState.SKELETON, null);
        if (value == null) {
            return null;
        }
        return value instanceof JsonNode node ? node : objectMapper.valueToTree(value);
    }
}
