-- ============================================
-- 验证管理员密码
-- ============================================
-- 
-- 此脚本用于验证数据库中存储的密码哈希值
-- 注意：BCrypt是不可逆的，无法直接从哈希值还原密码
-- 只能通过尝试验证来确认密码是否正确
--
-- 数据库中存储的密码哈希值：
-- $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO
--
-- 根据SQL脚本注释，这个哈希值应该是密码 "123456" 的BCrypt加密值

USE user_service_db;

-- 查看管理员账号信息
SELECT 
    id,
    username,
    role,
    password_hash,
    LENGTH(password_hash) as hash_length,
    SUBSTRING(password_hash, 1, 7) as hash_prefix,
    status,
    created_at
FROM user_credentials 
WHERE role = 'ADMIN' AND username = 'admin';

-- 密码哈希值格式说明：
-- 1. 前缀：$2a$10$ 表示使用BCrypt算法，强度为10
-- 2. 长度：60字符（固定）
-- 3. 如果前缀是 $2a$10$，长度是60，说明格式正确
-- 4. 要验证密码是否是 "123456"，需要在应用程序中通过 BCryptPasswordEncoder.matches() 方法验证

