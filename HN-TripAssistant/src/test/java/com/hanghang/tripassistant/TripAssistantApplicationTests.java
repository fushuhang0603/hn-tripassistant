package com.hanghang.tripassistant;

import com.hanghang.tripassistant.service.GaoDeMapMcpService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TripAssistantApplicationTests {

    @Autowired
    private GaoDeMapMcpService gaoDeMapMcpService;

    @Autowired
    private SyncMcpToolCallbackProvider mcpToolCallbackProvider;

    @Test
    void contextLoads() {
    }

    @Test
    void testListMcpTools() {
        ToolCallback[] callbacks = mcpToolCallbackProvider.getToolCallbacks();
        System.out.println("===== MCP 工具数量：" + callbacks.length + " =====");
        for (ToolCallback callback : callbacks) {
            System.out.println("工具名：" + callback.getToolDefinition().name());
        }
    }

    @Test
    void testWeather() {
        String result = gaoDeMapMcpService.weather("三亚");
        System.out.println("===== 天气查询结果 =====");
        System.out.println(result);
    }
}
