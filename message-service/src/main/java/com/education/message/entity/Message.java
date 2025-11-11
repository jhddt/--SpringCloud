package com.education.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息实体
 * 按照学习通消息权限划分机制设计
 */
@Data
@TableName("messages")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long messageId;
    
    /**
     * 发送者ID（学生ID或教师ID，来自对应服务）
     */
    private Long senderId;
    
    /**
     * 发送者类型：STUDENT, TEACHER, ADMIN, SYSTEM
     */
    private String senderType;
    
    /**
     * 接收者ID（学生ID或教师ID，或群ID，来自对应服务）
     * 对于群消息，receiverId为群ID
     * 对于系统通知，receiverId可以为null（通过scope_id和role_mask确定接收者）
     */
    private Long receiverId;
    
    /**
     * 接收者类型：STUDENT, TEACHER, ADMIN, GROUP
     */
    private String receiverType;
    
    /**
     * 消息类型：INSTANT_MESSAGE（即时消息）, SYSTEM_NOTICE（系统通知）,
     * INTERACTION_REMINDER（互动提醒）, PLATFORM_ANNOUNCEMENT（平台公告）
     */
    private String messageType;
    
    /**
     * 消息内容类型：TEXT（文本）, IMAGE（图片）, FILE（文件）
     */
    @TableField("content_type")
    private String contentType;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息范围类型：COURSE（课程）, GROUP（群组）, GLOBAL（全局）, PRIVATE（私聊）
     */
    @TableField("scope_type")
    private String scopeType;
    
    /**
     * 所属对象ID（课程ID / 群ID）
     * 用于上下文隔离和数据权限控制
     */
    @TableField("scope_id")
    private Long scopeId;
    
    /**
     * 可见角色标识（JSON数组格式，如：["teacher", "student"]）
     * 用于角色级别的权限控制
     */
    @TableField("role_mask")
    private String roleMask;
    
    /**
     * 状态：0-未读，1-已读
     */
    private Integer status;
    
    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField("updated_at")
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
    
    public LocalDateTime getUpdateTime() {
        return updatedAt;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updatedAt = updateTime;
    }
}
