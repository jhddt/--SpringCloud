package com.education.message.enums;

/**
 * 消息类型枚举
 * 按照学习通消息权限划分机制定义
 */
public enum MessageType {
    /**
     * 即时消息（师生交流、群聊）
     * 发送者：学生 / 教师 / 管理员
     * 接收者：同群成员
     */
    INSTANT_MESSAGE("INSTANT_MESSAGE", "即时消息"),
    
    /**
     * 系统通知（作业、考试）
     * 发送者：教师 / 教务 / 系统
     * 接收者：学生（课程成员）
     */
    SYSTEM_NOTICE("SYSTEM_NOTICE", "系统通知"),
    
    /**
     * 互动提醒（评论、点赞、签到）
     * 发送者：系统自动生成
     * 接收者：被评论 / 点赞 / 提交者
     */
    INTERACTION_REMINDER("INTERACTION_REMINDER", "互动提醒"),
    
    /**
     * 平台公告（课程通知、系统公告）
     * 发送者：管理员（院系/校级）
     * 接收者：所有选课用户或全校
     */
    PLATFORM_ANNOUNCEMENT("PLATFORM_ANNOUNCEMENT", "平台公告");
    
    private final String code;
    private final String description;
    
    MessageType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static MessageType fromCode(String code) {
        for (MessageType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return INSTANT_MESSAGE; // 默认返回即时消息
    }
}

