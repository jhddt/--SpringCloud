package com.education.student.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentDTO {
    private Long studentId;
    private String name;
    private String gender; // MALE, FEMALE
    private LocalDate dateOfBirth;
    private String major;
    private String grade;
    private String className;
    private String contactInfo; // JSON格式，包含phone和email
    private String avatarUrl;
    private Integer status;
    
    // 用于创建时的字段（从contactInfo中解析）
    private String phone;
    private String email;
    
    // 用于创建登录凭证的字段
    private String username; // 学号，作为用户名
    private String password; // 默认密码：123456
    
    // 用于关联现有user_credentials的字段（学生端创建个人信息时使用）
    private Long userId; // user_credentials表的id（当学生已登录但未完善个人信息时使用）
}

