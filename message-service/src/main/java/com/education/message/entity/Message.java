package com.education.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("messages")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long messageId;
    private Long senderId;
    private String senderType; // STUDENT, TEACHER, ADMIN
    private Long receiverId;
    private String receiverType; // STUDENT, TEACHER, ADMIN
    private String messageType; // TEXT, IMAGE, FILE, NOTIFICATION
    private String content;
    private Integer status; // 0-未读，1-已读
    private LocalDateTime createdAt;
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
