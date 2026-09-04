package com.hanghang.tripassistant.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TranslationNode implements NodeAction {

    @Autowired
    private ChatClient chatClient;


    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        //从state中获取句子
        String sentence = state.value("sentence","");
        PromptTemplate promptTemplate = new PromptTemplate("你是一个翻译专家,给定你一个句子你可以将其翻译，如果给定的句子是中文，你就翻译成英文，如果给定的句子是英文，你就翻译成中文"+
                "要求只返回翻译结果，不要返回其他信息。给定的句子{sentence}");
        promptTemplate.add("sentence", sentence);
        String prompt = promptTemplate.render();

        //模型调用
        String content = chatClient.prompt().user(prompt).call().content();

        //把翻译结果存入state
        return Map.of("translation",content);
    }
}
