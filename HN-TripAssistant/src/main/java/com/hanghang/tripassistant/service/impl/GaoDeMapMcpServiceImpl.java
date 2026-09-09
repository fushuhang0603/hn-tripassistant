package com.hanghang.tripassistant.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hanghang.tripassistant.service.GaoDeMapMcpService;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 高德地图 MCP 工具封装，供行程规划节点调用
 */
@Service
public class GaoDeMapMcpServiceImpl implements GaoDeMapMcpService {

    /**
     * 高德地图工具名
     */
    private static final String TOOL_WEATHER = "maps_weather";
    private static final String TOOL_GEO = "maps_geo";
    private static final String TOOL_TEXT_SEARCH = "maps_text_search";
    private static final String TOOL_AROUND_SEARCH = "maps_around_search";
    private static final String TOOL_SEARCH_DETAIL = "maps_search_detail";
    private static final String TOOL_DISTANCE = "maps_distance";
    private static final String TOOL_DRIVING = "maps_direction_driving";
    private static final String TOOL_WALKING = "maps_direction_walking";
    private static final String TOOL_TRANSIT = "maps_direction_transit_integrated";

    private final SyncMcpToolCallbackProvider mcpToolCallbackProvider;

    // 构造器注入MCP工具提供者
    public GaoDeMapMcpServiceImpl(SyncMcpToolCallbackProvider mcpToolCallbackProvider) {
        this.mcpToolCallbackProvider = mcpToolCallbackProvider;
    }

    @Override
    public String weather(String city) {
        return callTool(TOOL_WEATHER, Map.of("city", city));
    }

    @Override
    public String geo(String address, String city) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("address", address);
        if (StringUtils.hasText(city)) {
            args.put("city", city);
        }
        return callTool(TOOL_GEO, args);
    }

    @Override
    public String textSearch(String keywords, String city) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("keywords", keywords);
        if (StringUtils.hasText(city)) {
            args.put("city", city);
        }
        return callTool(TOOL_TEXT_SEARCH, args);
    }

    @Override
    public String aroundSearch(String location, String keywords) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("location", location);
        args.put("keywords", keywords);
        return callTool(TOOL_AROUND_SEARCH, args);
    }

    @Override
    public String searchDetail(String id) {
        return callTool(TOOL_SEARCH_DETAIL, Map.of("id", id));
    }

    @Override
    public String distance(String origin, String destination) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("origin", origin);
        args.put("destination", destination);
        return callTool(TOOL_DISTANCE, args);
    }

    @Override
    public String directionDriving(String origin, String destination) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("origin", origin);
        args.put("destination", destination);
        return callTool(TOOL_DRIVING, args);
    }

    @Override
    public String directionWalking(String origin, String destination) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("origin", origin);
        args.put("destination", destination);
        return callTool(TOOL_WALKING, args);
    }

    @Override
    public String directionTransit(String origin, String destination, String city, String cityd) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("origin", origin);
        args.put("destination", destination);
        if (StringUtils.hasText(city)) {
            args.put("city", city);
        }
        if (StringUtils.hasText(cityd)) {
            args.put("cityd", cityd);
        }
        return callTool(TOOL_TRANSIT, args);
    }

    /**
     * 按工具名匹配 ToolCallback 并调用，返回工具结果的原始文本。
     */
    private String callTool(String toolName, Map<String, Object> args) {
        ToolCallback[] callbacks = mcpToolCallbackProvider.getToolCallbacks();
        for (ToolCallback callback : callbacks) {
            if (toolName.equals(callback.getToolDefinition().name())) {
                String raw = callback.call(JSONUtil.toJsonStr(args));
                return extractText(raw);
            }
        }
        throw new IllegalStateException("未找到高德 MCP 工具：" + toolName);
    }

    /**
     * MCP 工具回调返回格式为 [{"text":"..."}]，这里提取出 text 原文。
     * 若格式不符合预期，则原样返回，避免二次破坏。
     */
    private String extractText(String raw) {
        try {
            JSONArray array = JSONUtil.parseArray(raw);
            if (array != null && !array.isEmpty()) {
                JSONObject first = array.getJSONObject(0);
                String text = first.getStr("text");
                if (text != null) {
                    return text;
                }
            }
        } catch (Exception ignored) {
            // 非数组格式，直接原样返回
        }
        return raw;
    }
}
