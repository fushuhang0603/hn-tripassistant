package com.hanghang.tripassistant.agent.graph.state;

/**
 * 行程规划图的状态键与模式常量。
 * OverAllState 本质是 Map，这里统一键名，节点间读写不写错字面量。
 */
public final class TripPlanState {

    /** 用户本轮消息 */
    public static final String USER_INPUT = "userInput";
    /** 合并后的槽位：cities / days / travelDate */
    public static final String SLOTS = "slots";
    /** 本轮模式：NEW 新规划 / ADJUST 调整已有行程 */
    public static final String MODE = "mode";
    /** 行程骨架（JSON）：days → items（time/place/note） */
    public static final String SKELETON = "skeleton";
    /** POI 校验补充信息（文本） */
    public static final String ENRICH = "enrich";
    /** 跨城交通补充信息（文本） */
    public static final String TRANSPORT = "transport";
    /** 最终展示给用户的文本 */
    public static final String OUTPUT_TEXT = "outputText";
    /** 缺参追问文案（非空 = 等待用户补参数） */
    public static final String ASK_MESSAGE = "askMessage";
    /** 是否等待用户确认（true = 草稿已出，等确认/修改） */
    public static final String NEED_CONFIRM = "needConfirm";

    /** 模式：新规划 */
    public static final String MODE_NEW = "NEW";
    /** 模式：调整已有行程 */
    public static final String MODE_ADJUST = "ADJUST";

    private TripPlanState() {
    }
}
