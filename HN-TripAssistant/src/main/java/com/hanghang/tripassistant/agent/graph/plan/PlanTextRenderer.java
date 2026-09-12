package com.hanghang.tripassistant.agent.graph.plan;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 行程骨架 → 用户可读 Markdown 文案。
 * 草稿与定稿共用一套渲染，仅标题口吻不同。
 */
@Component
public class PlanTextRenderer {

    /**
     * 渲染行程文案。
     *
     * @param skeleton    行程骨架 JSON：days 数组，每天含 day/city/theme/items
     * @param finalVersion true=定稿口吻；false=草稿口吻
     */
    public String render(JsonNode skeleton, boolean finalVersion) {
        if (skeleton == null || !skeleton.has("days")) {
            return "行程生成中……";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(finalVersion
                ? "### 你的海南行程定稿啦 🌴\n"
                : "### 行程草稿出炉，先看看 🌴\n");
        for (JsonNode day : skeleton.path("days")) {
            int dayNo = day.path("day").asInt();
            String city = day.path("city").asText("");
            String theme = day.path("theme").asText("");
            sb.append("\n**第 ").append(dayNo).append(" 天");
            if (StringUtils.hasText(city)) {
                sb.append(" · ").append(city);
            }
            sb.append("**");
            if (StringUtils.hasText(theme)) {
                sb.append("（").append(theme).append("）");
            }
            sb.append("\n");
            for (JsonNode item : day.path("items")) {
                String time = item.path("time").asText("");
                String place = item.path("place").asText("");
                String note = item.path("note").asText("");
                sb.append("- ").append(time).append(" ").append(place);
                if (StringUtils.hasText(note)) {
                    sb.append("：").append(note);
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
