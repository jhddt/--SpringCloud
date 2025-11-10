package com.education.message.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageDTO {
    private Long messageId;
    private Long senderId;
    private String senderType;
    private Long receiverId;
    private String receiverType;
    private String messageType;
    private String content;
    private Integer status;
    private String senderName;
    private String receiverName;
    private LocalDateTime createdAt;
    
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
