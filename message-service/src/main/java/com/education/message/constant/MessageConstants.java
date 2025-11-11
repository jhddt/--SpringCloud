package com.education.message.constant;

/**
 * 消息服务常量
 */
public class MessageConstants {
    
    /**
     * 消息类型权限映射
     * 定义哪些角色可以发送哪种类型的消息
     */
    public static final String[] INSTANT_MESSAGE_ALLOWED_ROLES = {
        "STUDENT", "TEACHER", "ADMIN"
    };
    
    public static final String[] SYSTEM_NOTICE_ALLOWED_ROLES = {
        "TEACHER", "ADMIN"
    };
    
    public static final String[] INTERACTION_REMINDER_ALLOWED_ROLES = {
        "SYSTEM" // 系统自动生成
    };
    
    public static final String[] PLATFORM_ANNOUNCEMENT_ALLOWED_ROLES = {
        "ADMIN"
    };
    
    /**
     * Redis Key前缀
     */
    public static final String REDIS_MESSAGE_PREFIX = "message:";
    public static final String REDIS_USER_SCOPE_PREFIX = "user:scope:";
    public static final String REDIS_COURSE_MEMBER_PREFIX = "course:member:";
    
    /**
     * WebSocket Channel前缀
     */
    public static final String WS_CHANNEL_USER_PREFIX = "/user/";
    public static final String WS_CHANNEL_COURSE_PREFIX = "/course/";
    public static final String WS_CHANNEL_GROUP_PREFIX = "/group/";
    
    /**
     * 消息状态
     */
    public static final int MESSAGE_STATUS_UNREAD = 0;
    public static final int MESSAGE_STATUS_READ = 1;
    
    /**
     * 角色掩码分隔符
     */
    public static final String ROLE_MASK_SEPARATOR = ",";
}

