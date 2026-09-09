package com.hanghang.tripassistant.agent.intent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "意图识别结果")
public class IntentResult {
    @Schema(description = "识别出的意图")
    private IntentType intent;

    @Schema(description = "置信度，0~1；低于0.5时强制降级为GENERAL")
    private double confidence;

    @Schema(description = "预提取槽位，如 city/days/date/keyword，供 Handler 直接消费")
    private Map<String, Object> slots;

    @Schema(description = "是否依赖会话状态；true 时路由前需加载 ChatContext")
    private boolean needContext;

    @Schema(description = "识别来源：RULE=规则短路，LLM=模型分类")
    private Source source;

    /**
     * 识别来源标记，便于日志排查与规则命中率统计
     */
    public enum Source {
        RULE,
        LLM
    }
}
