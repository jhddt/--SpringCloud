package com.education.user.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentDTO {
    private Long studentId;
    private String name;
    private String gender;
    private LocalDate dateOfBirth;
    private String major;
    private String grade;
    private String className;
    private String contactInfo; // JSON格式
    private String avatarUrl;
    private Integer status;
    
    // 兼容旧字段名
    public Long getId() {
        return studentId;
    }
    
    public void setId(Long id) {
        this.studentId = id;
    }
}

