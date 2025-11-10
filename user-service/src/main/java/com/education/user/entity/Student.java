package com.education.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("students")
public class Student {
    @TableId(type = IdType.AUTO)
    private Long studentId;
    private String name;
    private String gender; // MALE, FEMALE
    private LocalDate dateOfBirth;
    private String major;
    private String grade;
    private String className;
    private String contactInfo; // JSON格式
    private String avatarUrl;
    private Integer status; // 0-禁用，1-启用
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 兼容旧字段名
    public Long getId() {
        return studentId;
    }
    
    public void setId(Long id) {
        this.studentId = id;
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

