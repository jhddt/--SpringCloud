package com.education.message.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息DTO
 * 按照学习通消息权限划分机制设计
 */
@Data
public class MessageDTO {
    private Long messageId;
    
    /**
     * 发送者ID
     */
    private Long senderId;
    
    /**
     * 发送者类型：STUDENT, TEACHER, ADMIN, SYSTEM
     */
    private String senderType;
    
    /**
     * 发送者名称（从用户服务获取）
     */
    private String senderName;
    
    /**
     * 接收者ID（对于群消息，为群ID）
     */
    private Long receiverId;
    
    /**
     * 接收者类型：STUDENT, TEACHER, ADMIN, GROUP
     */
    private String receiverType;
    
    /**
     * 接收者名称（从用户服务获取）
     */
    private String receiverName;
    
    /**
     * 消息类型：INSTANT_MESSAGE, SYSTEM_NOTICE, INTERACTION_REMINDER, PLATFORM_ANNOUNCEMENT
     */
    private String messageType;
    
    /**
     * 消息内容类型：TEXT, IMAGE, FILE
     */
    private String contentType;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息范围类型：COURSE, GROUP, GLOBAL, PRIVATE
     */
    private String scopeType;
    
    /**
     * 所属对象ID（课程ID / 群ID）
     */
    private Long scopeId;
    
    /**
     * 可见角色标识（JSON数组格式）
     */
    private String roleMask;
    
    /**
     * 状态：0-未读，1-已读
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    // 兼容旧字段名
    public Long getId() {
        return messageId;
    }
    
    public void setId(Long id) {
        this.messageId = id;
    }
    
    public String getType() {
        return messageType;
    }
    
    public void setType(String type) {
        this.messageType = type;
    }
    
    public LocalDateTime getCreateTime() {
        return createdAt;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createdAt = createTime;
    }
}
