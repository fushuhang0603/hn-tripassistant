package com.hanghang.tripassistant.config;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;


@Configuration
public class GraphConfig {

    @Bean("quickStartGraph")
    public CompiledGraph quickStartGraph() throws GraphStateException {
        //构建策略工厂接口实现类
        KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactory() {
            @Override
            public Map<String, KeyStrategy> apply() {
                return Map.of("userInput",new ReplaceStrategy());
            }
        };

        //构建节点
        var nodeA = node_async(state -> {
            return Map.of("userInput", "初始值");
        });

        var nodeB = node_async(state -> {
            return Map.of("userInput", "更新后的值");
        });

        //构建图
        StateGraph stateGraph = new StateGraph(keyStrategyFactory)
                .addNode("nodeA",nodeA)
                .addNode("nodeB",nodeB)
                .addEdge(START,"nodeA")
                .addEdge("nodeA", "nodeB")
                .addEdge("nodeB", END);

        //编译图
        return stateGraph.compile();
    }
}
