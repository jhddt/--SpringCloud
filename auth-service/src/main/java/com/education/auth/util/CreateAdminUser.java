package com.education.auth.util;

import com.education.auth.entity.User;
import com.education.auth.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 创建admin用户的工具类
 * 用于临时创建或重置admin用户
 * 
 * 使用方法：
 * 1. 在AuthService中注入此工具类
 * 2. 调用 createAdminUser() 方法
 * 3. 或者直接运行main方法生成密码哈希值，然后手动更新数据库
 */
@Component
public class CreateAdminUser {
    
    @Autowired
    private UserMapper userMapper;
    
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
    
    /**
     * 创建或重置admin用户
     * @param password 密码（默认：123456）
     */
    public void createAdminUser(String password) {
        // 删除现有的admin用户
        userMapper.delete(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, "admin")
        );
        
        // 创建新的admin用户
        User admin = new User();
        admin.setUsername("admin");
        admin.setPasswordHash(encoder.encode(password));
        admin.setRole("ADMIN");
        admin.setStatus(1);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        
        userMapper.insert(admin);
        
        System.out.println("Admin用户创建成功！");
        System.out.println("用户名: admin");
        System.out.println("密码: " + password);
        System.out.println("密码哈希值: " + admin.getPasswordHash());
        System.out.println("密码哈希值长度: " + admin.getPasswordHash().length());
    }
    
    /**
     * 生成密码哈希值（用于手动更新数据库）
     */
    public static void main(String[] args) {
        String password = "123456";
        String passwordHash = encoder.encode(password);
        
        System.out.println("==========================================");
        System.out.println("生成admin用户密码哈希值");
        System.out.println("==========================================");
        System.out.println("密码: " + password);
        System.out.println("BCrypt哈希值: " + passwordHash);
        System.out.println("哈希值长度: " + passwordHash.length());
        System.out.println("==========================================");
        System.out.println();
        System.out.println("SQL更新语句：");
        System.out.println("DELETE FROM `user_credentials` WHERE `username` = 'admin';");
        System.out.println("INSERT INTO `user_credentials` (`student_id`, `teacher_id`, `username`, `password_hash`, `role`, `status`)");
        System.out.println("VALUES (NULL, NULL, 'admin', '" + passwordHash + "', 'ADMIN', 1);");
        System.out.println();
        System.out.println("验证：");
        boolean matches = encoder.matches(password, passwordHash);
        System.out.println("密码验证结果: " + (matches ? "✓ 匹配" : "✗ 不匹配"));
    }
}

