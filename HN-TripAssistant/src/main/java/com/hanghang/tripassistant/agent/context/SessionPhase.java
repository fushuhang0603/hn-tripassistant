package com.hanghang.tripassistant.agent.context;

/**
 * 会话状态机（设计文档 6.4 简版）。
 * 当前链路只用 IDLE / COLLECTING；PLANNING / PLAN_DONE 待行程编排 Agent 接入后启用。
 */
public enum SessionPhase {
    /** 空闲：无进行中的任务 */
    IDLE,
    /** 收集中：Handler 缺参追问中，等待用户补齐参数 */
    COLLECTING,
    /** 编排中：参数齐全，正在生成行程 */
    PLANNING,
    /** 编排完成：行程已生成，可接受调整指令 */
    PLAN_DONE
}
