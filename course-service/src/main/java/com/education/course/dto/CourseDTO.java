package com.education.course.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CourseDTO {
    private Long courseId;
    private String courseName;
    private String courseCode;
    private String courseDescription;
    private BigDecimal credit;
    private Long teacherId;
    private String teacherName;
    private String department;
    private Integer totalCapacity;
    private Integer selectedCount;
    private String coverImage;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
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
}
