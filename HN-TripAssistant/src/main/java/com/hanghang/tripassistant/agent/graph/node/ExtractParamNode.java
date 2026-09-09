package com.hanghang.tripassistant.agent.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.hanghang.tripassistant.agent.graph.state.ExtractParamState;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ExtractParamNode implements NodeAction {

    @Autowired
    private ChatClient chatClient;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String userInput = state.value("userInput", "");

        BeanOutputConverter<ExtractParamState> converter =
                new BeanOutputConverter<>(new ParameterizedTypeReference<ExtractParamState>() {});

        // 让模型严格按 JSON 结构输出，返回的内容再交给 converter 解析
        String content = chatClient.prompt()
                .system(s -> s.text(
                        "你是海南旅游出行信息提取助手。请从用户描述中提取出行基本信息，" +
                                "并严格按照以下 JSON 结构输出，不要输出任何多余文字、解释或 Markdown：\n" +
                                converter.getFormat()))
                .user(userInput)
                .call()
                .content();

        ExtractParamState extractParam = converter.convert(content);

        // 写回状态
        Map<String, Object> result = new HashMap<>();
        result.put("extractParam", extractParam);
        return result;
    }
}
