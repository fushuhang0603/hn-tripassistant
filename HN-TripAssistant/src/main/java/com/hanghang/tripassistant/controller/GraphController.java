package com.hanghang.tripassistant.controller;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/graph")
public class GraphController {

    @Autowired
    @Qualifier("quickStartGraph")
    private CompiledGraph compiledGraph;

    @Autowired
    @Qualifier("SentenceGraph")
    private CompiledGraph sentenceGraph;

    @Autowired
    private ChatClient chatClient;

    @GetMapping("/quickStartGraph")
    public String TestGraph(){
        RunnableConfig config = RunnableConfig.builder()
                .threadId("replace-strategy-demo")
                .build();

        Optional<OverAllState> invoke = compiledGraph.invoke(Map.of(), config);
        //System.out.println(invoke.toString());
        String value = invoke.get().value("userInput", "default-value");
        System.out.println("最终状态中的 value: " + value);
        return "ok";
    }

    @GetMapping("/sentenceGraph")
    public String TestSentenceGraph(@RequestParam("word") String word){
        RunnableConfig config = RunnableConfig.builder()
                .threadId("sentence-strategy-demo")
                .build();
        Optional<OverAllState> invoke = sentenceGraph.invoke(Map.of("word", word), config);
        // 返回翻译结果（TranslationNode 写入 state 的 translation 字段）
        String translation = invoke.get().value("translation", "");
        return translation;
    }

    @GetMapping("/chat")
    public String testChat(@RequestParam("question") String question){
        String response = chatClient.prompt().user(question).call().content();
        System.out.println(response);
        return "ok";
    }
}
