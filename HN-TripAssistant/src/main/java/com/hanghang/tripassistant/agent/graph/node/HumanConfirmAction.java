package com.hanghang.tripassistant.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanghang.tripassistant.agent.graph.plan.PlanTextRenderer;
import com.hanghang.tripassistant.agent.graph.state.TripPlanState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 节点6：人机确认中断点（软中断）。
 * 渲染行程文案（草稿/定稿两种口吻），附带 POI 校验与交通补充信息。
 * needConfirm=true → 图正常结束，Handler 据此发 ask 事件等待用户确认/修改；
 * needConfirm=false（用户已确认）→ 放行 finalize 定稿。
 */
@Component
public class HumanConfirmAction implements NodeAction {

    @Autowired
    private PlanTextRenderer planTextRenderer;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        boolean needConfirm = state.value(TripPlanState.NEED_CONFIRM, true);
        JsonNode skeleton = skeleton(state);
        String text = planTextRenderer.render(skeleton, !needConfirm);

        StringBuilder sb = new StringBuilder(text);
        appendExtra(sb, state.value(TripPlanState.TRANSPORT, ""));
        appendExtra(sb, state.value(TripPlanState.ENRICH, ""));
        if (needConfirm) {
            sb.append("\n---\n行程草稿如上～哪里想调整直接说（比如「第二天改成海边为主」），确认就说「可以」");
        }
        return Map.of(TripPlanState.OUTPUT_TEXT, sb.toString());
    }

    private void appendExtra(StringBuilder sb, String extra) {
        if (StringUtils.hasText(extra)) {
            sb.append("\n").append(extra);
        }
    }

    private JsonNode skeleton(OverAllState state) {
        Object value = state.value(TripPlanState.SKELETON, null);
        if (value == null) {
            return null;
        }
        return value instanceof JsonNode node ? node : objectMapper.valueToTree(value);
    }
}
