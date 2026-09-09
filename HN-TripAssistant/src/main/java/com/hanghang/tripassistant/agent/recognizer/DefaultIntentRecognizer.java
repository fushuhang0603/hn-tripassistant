package com.hanghang.tripassistant.agent.recognizer;

import com.hanghang.tripassistant.agent.intent.IntentResult;
import com.hanghang.tripassistant.agent.intent.IntentType;
import com.hanghang.tripassistant.agent.request.ChatContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 意图识别器默认实现：规则段短路 → LLM 段分类 → 低置信度降级 GENERAL。
 * 识别器无状态；追问应答/行程调整等依赖会话状态的判定，待 SessionManager 接入后扩展。
 */
@Slf4j
@Component
public class DefaultIntentRecognizer implements IntentRecognizer {

    private final IntentRuleEngine ruleEngine;
    private final LLMIntentClassifier llmClassifier;

    public DefaultIntentRecognizer(IntentRuleEngine ruleEngine, LLMIntentClassifier llmClassifier) {
        this.ruleEngine = ruleEngine;
        this.llmClassifier = llmClassifier;
    }

    @Override
    public IntentResult recognize(String message, ChatContext context) {
        // 1. 规则段：高频确定性意图，0 token 短路
        IntentResult ruleResult = ruleEngine.match(message);
        if (ruleResult != null) {
            log.info("[意图识别] 规则命中 intent={}, slots={}, message={}",
                    ruleResult.getIntent(), ruleResult.getSlots(), message);
            return ruleResult;
        }

        // 2. LLM 段：未命中时模型分类
        IntentResult llmResult = llmClassifier.classify(message);
        log.info("[意图识别] LLM 分类 intent={}, confidence={}, message={}",
                llmResult.getIntent(), llmResult.getConfidence(), message);

        // 3. 低置信度强制降级 GENERAL
        if (llmResult.getConfidence() < 0.5) {
            llmResult.setIntent(IntentType.GENERAL);
        }
        return llmResult;
    }
}
