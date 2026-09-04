package com.hanghang.tripassistant.utils;

import com.hanghang.tripassistant.common.UserBasicInfo;
import com.hanghang.tripassistant.domain.entiy.User;


public class UserContext {

    private static final ThreadLocal<UserBasicInfo> HOLDER = new ThreadLocal<>();

    /** 写入当前线程的用户信息（登录成功后由 LoginInterceptor 调用） */
    public static void set(UserBasicInfo info) {
        HOLDER.set(info);
    }

    /** 获取当前线程的用户信息，未登录时为 null */
    public static UserBasicInfo get() {
        return HOLDER.get();
    }

    /** 获取当前登录用户 id，未登录返回 null */
    public static Long getUserId() {
        UserBasicInfo info = HOLDER.get();
        return info == null ? null : info.getId();
    }

    /** 获取当前登录用户名，未登录返回 null */
    public static String getUsername() {
        UserBasicInfo info = HOLDER.get();
        return info == null ? null : info.getUsername();
    }

    public static String getUserRole(){
        UserBasicInfo info = HOLDER.get();
        return info == null ? null : info.getRole();
    }

    /** 清理当前线程变量（请求结束时由 LoginInterceptor 调用，防止内存泄漏/数据串线） */
    public static void clear() {
        HOLDER.remove();
    }
}
