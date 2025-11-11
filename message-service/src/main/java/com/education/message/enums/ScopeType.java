package com.education.message.enums;

/**
 * 消息范围类型枚举
 * 用于上下文隔离
 */
public enum ScopeType {
    /**
     * 课程范围
     */
    COURSE("COURSE", "课程"),
    
    /**
     * 群组范围
     */
    GROUP("GROUP", "群组"),
    
    /**
     * 全局范围（平台公告）
     */
    GLOBAL("GLOBAL", "全局"),
    
    /**
     * 私聊范围
     */
    PRIVATE("PRIVATE", "私聊");
    
    private final String code;
    private final String description;
    
    ScopeType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static ScopeType fromCode(String code) {
        for (ScopeType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return PRIVATE; // 默认返回私聊
    }
}

