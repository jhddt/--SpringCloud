package com.education.auth.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码生成工具（用于生成 BCrypt 密码）
 * 运行此类的 main 方法可以生成密码 "123456" 的 BCrypt 编码值
 */
public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "123456";
        String encoded = encoder.encode(password);
        System.out.println("密码: " + password);
        System.out.println("BCrypt 编码: " + encoded);
        System.out.println("验证: " + encoder.matches(password, encoded));
    }
}

