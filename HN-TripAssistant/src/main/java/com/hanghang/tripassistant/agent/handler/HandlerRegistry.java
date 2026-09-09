package com.hanghang.tripassistant.agent.handler;

import com.hanghang.tripassistant.agent.intent.IntentType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Handler 注册表：Spring 启动时自动收集所有 IntentHandler Bean，
 * 建立意图-Handler 索引，负责路由分发与兜底降级。
 */
@Component
public class HandlerRegistry {
    private final Map<IntentType, IntentHandler> handlers = new EnumMap<>(IntentType.class);

    /**
     * 构造注入全部 Handler
     * 同一意图出现多个 Handler 属配置错误，直接启动失败暴露问题。
     */
    public HandlerRegistry(List<IntentHandler> handlerList) {
        for (IntentHandler handler : handlerList) {
            IntentType type = handler.support();
            if (handlers.containsKey(type)) {
                throw new IllegalStateException("意图 [" + type + "] 注册了多个 Handler，请检查");
            }
            handlers.put(type, handler);
        }
    }

    /**
     * 按意图分发；意图未注册或为 null 时降级到 GENERAL 兜底。
     */
    public IntentHandler dispatch(IntentType intent) {
        IntentHandler handler = intent == null ? null : handlers.get(intent);
        return handler != null ? handler : handlers.get(IntentType.GENERAL);
    }

    /** 已注册意图数量，用于启动日志与健康检查 */
    public int size() {
        return handlers.size();
    }

}
