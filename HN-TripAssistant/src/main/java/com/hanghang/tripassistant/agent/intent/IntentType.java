package com.hanghang.tripassistant.agent.intent;

import lombok.Getter;

@Getter
public enum IntentType {

    /**
     * 行程编排：新建/调整行程
     */
    TRIP_PLANNING("行程编排"),

    /**
     *
     */
    TRANSPORT_SEARCH("交通方式"),

    /**
     * 住宿查询：酒店、民宿查询
     */
    HOTEL_SEARCH("住宿查询"),

    /**
     * POI查询：景点、美食游玩地点查询
     */
    POI_SEARCH("景点美食查询"),

    /**
     * 天气查询
     */
    WEATHER("天气查询"),

    /**
     * 政策问答，离岛免税，登岛轮渡等问题
     */
    POLICY_QA("政策问答"),

    /**
     * 攻略知识库检索
     */
    RAG_QA("攻略检索"),

    /**
     * 未注册意图，低置信度，用于异常降级
     */
    GENERAL("闲聊");

    private final String intent;

    IntentType(String intent){
        this.intent = intent;
    }

}
