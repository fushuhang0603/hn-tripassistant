package com.hanghang.tripassistant.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanghang.tripassistant.agent.graph.state.TripPlanState;
import com.hanghang.tripassistant.service.GaoDeMapMcpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 节点3：POI 真实性校验（高德 textSearch）。
 * 对骨架中每天前 2 个地点做关键词搜索，确认存在并补充地址，
 * 校验结果以文本形式拼入 enrich，供最终文案附加说明。
 * 校验失败不阻塞主流程（try-catch 兜底）。
 */
@Slf4j
@Component
public class PoiEnrichAction implements NodeAction {

    /** 每天最多校验的地点数，控制工具调用量 */
    private static final int MAX_PER_DAY = 2;

    @Autowired
    private GaoDeMapMcpService gaoDeMapMcpService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        JsonNode skeleton = skeleton(state);
        if (skeleton == null) {
            return Map.of();
        }
        StringBuilder sb = new StringBuilder();
        int checked = 0;
        for (JsonNode day : skeleton.path("days")) {
            String city = day.path("city").asText("");
            for (JsonNode item : day.path("items")) {
                if (checked >= MAX_PER_DAY) {
                    break;
                }
                String place = item.path("place").asText("");
                if (!StringUtils.hasText(place)) {
                    continue;
                }
                checked++;
                String address = verifyPoi(place, city);
                if (address != null) {
                    sb.append("✓ ").append(place).append("（").append(address).append("）\n");
                } else {
                    sb.append("? ").append(place).append("（地图未精确命中，出发前再确认下）\n");
                }
            }
        }
        if (sb.isEmpty()) {
            return Map.of();
        }
        return Map.of(TripPlanState.ENRICH, sb.toString());
    }

    /** 校验单个地点：命中返回地址，未命中/异常返回 null */
    private String verifyPoi(String place, String city) {
        try {
            String resultJson = gaoDeMapMcpService.textSearch(place, city);
            JsonNode root = objectMapper.readTree(resultJson);
            JsonNode pois = root.path("pois");
            if (pois.isArray() && !pois.isEmpty()) {
                JsonNode first = pois.get(0);
                String name = first.path("name").asText("");
                String address = first.path("address").asText("");
                String detail = StringUtils.hasText(address) ? address : name;
                return StringUtils.hasText(detail) ? detail : null;
            }
        } catch (Exception e) {
            log.warn("[行程规划] POI 校验失败 place={}：{}", place, e.getMessage());
        }
        return null;
    }

    private JsonNode skeleton(OverAllState state) {
        Object value = state.value(TripPlanState.SKELETON, null);
        if (value == null) {
            return null;
        }
        return value instanceof JsonNode node ? node : objectMapper.valueToTree(value);
    }
}
