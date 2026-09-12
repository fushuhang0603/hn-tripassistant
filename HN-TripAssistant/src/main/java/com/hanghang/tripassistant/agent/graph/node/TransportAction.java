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
 * 节点4：跨城交通补全（高德 geo × 2 + directionDriving）。
 * 骨架中相邻两天城市不同 = 跨城，测算驾车距离与用时，
 * 拼入 transport 供最终文案附加说明。无跨城或调用失败跳过。
 */
@Slf4j
@Component
public class TransportAction implements NodeAction {

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
        String prevCity = null;
        for (JsonNode day : skeleton.path("days")) {
            String city = day.path("city").asText("");
            if (StringUtils.hasText(prevCity) && StringUtils.hasText(city) && !prevCity.equals(city)) {
                String info = drivingInfo(prevCity, city);
                if (info != null) {
                    sb.append(info).append("\n");
                }
            }
            prevCity = city;
        }
        if (sb.isEmpty()) {
            return Map.of();
        }
        return Map.of(TripPlanState.TRANSPORT, sb.toString());
    }

    /** 两个城市间驾车距离/用时；失败返回 null */
    private String drivingInfo(String fromCity, String toCity) {
        try {
            String fromLoc = resolveLocation(fromCity);
            String toLoc = resolveLocation(toCity);
            if (fromLoc == null || toLoc == null) {
                return null;
            }
            String resultJson = gaoDeMapMcpService.directionDriving(fromLoc, toLoc);
            JsonNode root = objectMapper.readTree(resultJson);
            JsonNode paths = root.path("route").path("paths");
            if (paths.isArray() && !paths.isEmpty()) {
                JsonNode first = paths.get(0);
                double distanceMeters = first.path("distance").asDouble(0);
                double durationSeconds = first.path("duration").asDouble(0);
                if (distanceMeters > 0) {
                    long km = Math.round(distanceMeters / 1000);
                    long minutes = Math.round(durationSeconds / 60);
                    return "🚗 " + fromCity + " → " + toCity + "：约 " + km + " 公里，"
                            + (minutes >= 60 ? (minutes / 60) + " 小时 " + (minutes % 60) + " 分钟" : minutes + " 分钟");
                }
            }
        } catch (Exception e) {
            log.warn("[行程规划] 跨城交通测算失败 {} → {}：{}", fromCity, toCity, e.getMessage());
        }
        return null;
    }

    /** 城市名 → 经纬度 "lon,lat"；失败返回 null */
    private String resolveLocation(String city) {
        try {
            String geoJson = gaoDeMapMcpService.geo(city, null);
            JsonNode root = objectMapper.readTree(geoJson);
            JsonNode geocodes = root.path("geocodes");
            if (geocodes.isArray() && !geocodes.isEmpty()) {
                String location = geocodes.get(0).path("location").asText(null);
                if (StringUtils.hasText(location)) {
                    return location;
                }
            }
        } catch (Exception e) {
            log.warn("[行程规划] 城市地理编码失败 city={}：{}", city, e.getMessage());
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
