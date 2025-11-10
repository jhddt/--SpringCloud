package com.education.selection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("enrollments")
public class Enrollment {
    @TableId(type = IdType.AUTO)
    private Long enrollmentId;
    private Long studentId;
    private Long courseId;
    private LocalDateTime enrollmentTime;
    private Integer status; // 0-已选，1-已退
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 兼容旧字段名
    public Long getId() {
        return enrollmentId;
    }
    
    public void setId(Long id) {
        this.enrollmentId = id;
    }
    
    public LocalDateTime getSelectionTime() {
        return enrollmentTime;
    }
    
    public void setSelectionTime(LocalDateTime selectionTime) {
        this.enrollmentTime = selectionTime;
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

