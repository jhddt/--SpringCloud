package com.education.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_credentials")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("student_id")
    private Long studentId; // 学生ID（当role=STUDENT时）
    @TableField("teacher_id")
    private Long teacherId; // 教师ID（当role=TEACHER时）
    private String username; // 用户名（学号/工号/管理员账号）
    private String passwordHash; // 对应数据库的password_hash字段（使用passwordHash以配合map-underscore-to-camel-case自动映射）
    private String email;
    private String phone;
    private String role; // ADMIN, TEACHER, STUDENT
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;
    private Integer status; // 0-禁用，1-启用
    @TableField("created_at")
    private LocalDateTime createdAt; // 对应数据库的created_at字段
    @TableField("updated_at")
    private LocalDateTime updatedAt; // 对应数据库的updated_at字段
    
    // 兼容旧字段名（用于代码中可能还在使用createTime/updateTime的地方）
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

