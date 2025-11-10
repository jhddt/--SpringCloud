package com.education.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码工具类
 * 统一管理密码加密和验证，确保所有服务使用相同的加密方式
 */
public class PasswordUtil {
    
    /**
     * BCrypt强度参数（默认10，范围4-31）
     * 值越大，加密越安全，但计算时间越长
     * 10是推荐的平衡值
     */
    private static final int BCRYPT_STRENGTH = 10;
    
    /**
     * 统一的PasswordEncoder实例
     * 所有服务都应该使用这个实例来确保密码加密一致性
     */
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    
    /**
     * 获取统一的PasswordEncoder实例
     * @return PasswordEncoder实例
     */
    public static PasswordEncoder getPasswordEncoder() {
        return PASSWORD_ENCODER;
    }
    
    /**
     * 加密密码
     * @param rawPassword 原始密码
     * @return 加密后的密码哈希值
     */
    public static String encode(String rawPassword) {
        return PASSWORD_ENCODER.encode(rawPassword);
    }
    
    /**
     * 验证密码
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码哈希值
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        
        // 清理密码中的空白字符和换行符（防止数据库存储时引入的额外字符）
        String cleanedPassword = encodedPassword.trim()
                .replace("\n", "")
                .replace("\r", "")
                .replace("\t", "");
        
        try {
            return PASSWORD_ENCODER.matches(rawPassword, cleanedPassword);
        } catch (IllegalArgumentException e) {
            // BCrypt格式错误
            return false;
        }
    }
    
    /**
     * 生成默认密码（123456）的加密值
     * 用于数据库初始化脚本
     * @return 加密后的密码哈希值
     */
    public static String getDefaultPasswordHash() {
        return encode("123456");
    }
    
    /**
     * 测试方法：生成密码哈希值
     * 运行此方法可以生成指定密码的BCrypt哈希值，用于数据库初始化
     */
    public static void main(String[] args) {
        String password = "123456";
        String hash = encode(password);
        System.out.println("密码: " + password);
        System.out.println("BCrypt哈希值: " + hash);
        System.out.println("验证结果: " + matches(password, hash));
    }
}

