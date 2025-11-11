package com.education.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("courses")
public class Course {
    @TableId(type = IdType.AUTO)
    private Long courseId;
    private String courseName;
    private String courseCode;
    private String courseDescription;
    private BigDecimal credit;
    private Long teacherId;
    @TableField("teacher_name")
    private String teacherName;
    private String department;
    private Integer totalCapacity;
    private Integer selectedCount;
    private String coverImage;
    private Integer status; // 0-未开放，1-开放选课，2-已结束
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 兼容旧字段名
    public Long getId() {
        return courseId;
    }
    
    public void setId(Long id) {
        this.courseId = id;
    }
    
    public String getDescription() {
        return courseDescription;
    }
    
    public void setDescription(String description) {
        this.courseDescription = description;
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
