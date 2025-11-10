-- ============================================
-- 验证管理员密码哈希值
-- ============================================

USE user_service_db;

-- 查看管理员账号的密码哈希值
SELECT 
    id,
    username,
    role,
    password_hash,
    LENGTH(password_hash) as hash_length,
    SUBSTRING(password_hash, 1, 7) as hash_prefix,
    CASE 
        WHEN password_hash LIKE '$2a$10$%' AND LENGTH(password_hash) = 60 THEN '✓ 格式正确'
        ELSE '✗ 格式错误'
    END as format_check,
    status
FROM user_credentials 
WHERE username = 'admin' AND role = 'ADMIN';

-- 密码哈希值说明：
-- 1. 格式：$2a$10$...（60字符）
-- 2. $2a$ 表示 BCrypt 算法版本
-- 3. 10 表示强度（cost factor）
-- 4. 这个哈希值对应密码：123456
-- 5. 要验证密码是否正确，需要在应用程序中使用 BCryptPasswordEncoder.matches() 方法

-- 注意：BCrypt 是不可逆的，无法直接从哈希值还原密码
-- 只能通过 BCryptPasswordEncoder.matches("123456", password_hash) 来验证

