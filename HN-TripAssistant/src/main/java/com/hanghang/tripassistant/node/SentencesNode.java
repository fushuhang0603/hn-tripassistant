package com.hanghang.tripassistant.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class SentencesNode implements NodeAction {
    @Autowired
    private ChatClient chatClient;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        //从state中获取单词
        String word = state.value("word","");
        PromptTemplate promptTemplate = new PromptTemplate("你是一个英语造句专家,能够基于给定的单词进行造句"+
                "要求只返回造好的句子，不要返回其他信息。给定的单词{word}");
        promptTemplate.add("word", word);
        String prompt = promptTemplate.render();

        //模型调用
        String content = chatClient.prompt().user(prompt).call().content();

        //把句子存入state
        return Map.of("sentence",content);
    }
}
