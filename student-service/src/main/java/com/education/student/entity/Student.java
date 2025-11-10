package com.education.student.entity;

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
}

