package com.hanghang.tripassistant.agent.request;

import lombok.Data;

/**
 * SSE 流式事件：/api/chat/send/stream 的统一事件模型。
 * type 取值：meta（会话元信息）/ token（文本增量）/ ask（追问）/ error（错误兜底）/ end（结束+结构化数据）。
 */
@Data
public class StreamEvent {

    /** 事件类型：meta / token / ask / error / end */
    private String type;

    /** meta：会话ID，前端持久化带回 */
    private String sessionId;

    /** meta：实际执行的意图（IntentType 名） */
    private String intent;

    /** token / error：文本内容 */
    private String content;

    /** ask：完整追问回复文案 */
    private String reply;

    /** ask：追问提示（前端引导用户继续输入） */
    private String askMessage;

    /** end：结构化数据，如天气原始 JSON */
    private Object data;

    public static StreamEvent meta(String sessionId, String intent) {
        StreamEvent event = new StreamEvent();
        event.setType("meta");
        event.setSessionId(sessionId);
        event.setIntent(intent);
        return event;
    }

    public static StreamEvent token(String content) {
        StreamEvent event = new StreamEvent();
        event.setType("token");
        event.setContent(content);
        return event;
    }

    public static StreamEvent ask(String reply, String askMessage) {
        StreamEvent event = new StreamEvent();
        event.setType("ask");
        event.setReply(reply);
        event.setAskMessage(askMessage);
        return event;
    }

    public static StreamEvent error(String content) {
        StreamEvent event = new StreamEvent();
        event.setType("error");
        event.setContent(content);
        return event;
    }

    public static StreamEvent end(Object data) {
        StreamEvent event = new StreamEvent();
        event.setType("end");
        event.setData(data);
        return event;
    }
}
