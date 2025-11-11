package com.education.teacher.dto;

import lombok.Data;

@Data
public class TeacherDTO {
    private Long teacherId;
    private String name;
    private String gender;
    private String department;
    private String title;
    private String contactInfo; // JSON格式
    private String avatarUrl;
    private Integer status;
    
    // 用于创建时的字段（从contactInfo中解析）
    private String phone;
    private String email;
    
    // 用于创建登录凭证的字段
    private String username; // 工号，作为用户名
    private String password; // 默认密码：123456
    
    // 用于关联现有user_credentials的字段（用于消息服务）
    private Long userId; // user_credentials表的id
    
    // 兼容旧字段名
    public Long getId() {
        return teacherId;
    }
    
    public void setId(Long id) {
        this.teacherId = id;
    }
    
    // 兼容前端字段名
    public String getRealName() {
        return name;
    }
    
    public void setRealName(String realName) {
        this.name = realName;
    }
    
    public String getAvatar() {
        return avatarUrl;
    }
    
    public void setAvatar(String avatar) {
        this.avatarUrl = avatar;
    }
    
    public String getTeacherNo() {
        return username;
    }
    
    public void setTeacherNo(String teacherNo) {
        this.username = teacherNo;
    }
}
