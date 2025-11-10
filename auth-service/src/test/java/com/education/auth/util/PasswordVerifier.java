package com.education.auth.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码验证工具
 * 用于验证数据库中存储的密码哈希值是否对应指定的密码
 */
public class PasswordVerifier {
    
    public static void main(String[] args) {
        // 数据库中存储的密码哈希值
        String storedHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO";
        
        // 要验证的密码
        String passwordToVerify = "123456";
        
        // 创建BCryptPasswordEncoder（强度10）
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        
        // 验证密码
        boolean matches = encoder.matches(passwordToVerify, storedHash);
        
        System.out.println("==========================================");
        System.out.println("密码验证结果");
        System.out.println("==========================================");
        System.out.println("存储的哈希值: " + storedHash);
        System.out.println("要验证的密码: " + passwordToVerify);
        System.out.println("验证结果: " + (matches ? "✓ 匹配" : "✗ 不匹配"));
        System.out.println("==========================================");
        
        // 额外验证：尝试其他常见密码
        String[] commonPasswords = {"admin", "password", "12345678", "123123"};
        System.out.println("\n尝试其他常见密码:");
        for (String pwd : commonPasswords) {
            boolean result = encoder.matches(pwd, storedHash);
            System.out.println("  密码 \"" + pwd + "\": " + (result ? "✓ 匹配" : "✗ 不匹配"));
        }
        
        // 生成新的123456的哈希值（用于对比）
        System.out.println("\n生成新的 '123456' 密码哈希值（用于对比）:");
        String newHash = encoder.encode("123456");
        System.out.println("新哈希值: " + newHash);
        System.out.println("新哈希值验证: " + encoder.matches("123456", newHash));
        System.out.println("\n注意：BCrypt每次加密都会产生不同的哈希值（因为随机salt），");
        System.out.println("但都能通过matches()方法验证原始密码。");
    }
}

