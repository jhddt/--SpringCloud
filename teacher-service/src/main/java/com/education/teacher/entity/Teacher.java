package com.education.teacher.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("teachers")
public class Teacher {
    @TableId(type = IdType.AUTO)
    private Long teacherId;
    private String name;
    private String gender; // MALE, FEMALE
    private String department;
    private String title;
    private String contactInfo; // JSON格式
    private String avatarUrl;
    private Integer status; // 0-禁用，1-启用
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 兼容旧字段名
    public Long getId() {
        return teacherId;
    }
    
    public void setId(Long id) {
        this.teacherId = id;
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
