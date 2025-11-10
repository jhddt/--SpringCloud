package com.education.teacher.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_credentials")
public class UserCredential {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("student_id")
    private Long studentId; // 学生ID（当role=STUDENT时）
    @TableField("teacher_id")
    private Long teacherId; // 教师ID（当role=TEACHER时，来自teacher-service）
    private String username; // 用户名（学号/工号/管理员账号）
    @TableField("password_hash")
    private String passwordHash; // 密码哈希值（BCrypt）
    private String email;
    private String phone;
    private String role; // ADMIN, TEACHER, STUDENT
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;
    private Integer status; // 0-禁用，1-启用
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}

