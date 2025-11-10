-- 检查数据库中的密码哈希值
-- 用于诊断密码验证问题

USE `user_service_db`;

-- 查询admin用户的密码哈希值及其详细信息
SELECT 
    `id`,
    `username`,
    `password_hash`,
    LENGTH(`password_hash`) as password_length,
    CHAR_LENGTH(`password_hash`) as password_char_length,
    LEFT(`password_hash`, 7) as password_prefix,
    SUBSTRING(`password_hash`, 1, 7) as password_start,
    `role`,
    `status`
FROM `user_credentials`
WHERE `username` = 'admin';

-- 验证密码哈希值格式
-- BCrypt哈希值应该：
-- 1. 以 $2a$10$ 开头（7个字符）
-- 2. 总长度为60字符
-- 3. 后面跟着53个字符的哈希值

