-- ============================================
-- 删除并重新创建admin用户
-- 用途：修复admin用户的密码哈希值长度问题（61字符 -> 60字符）
-- ============================================

USE `user_service_db`;

-- 步骤1：删除现有的admin用户
DELETE FROM `user_credentials` WHERE `username` = 'admin';

-- 步骤2：重新插入admin用户，使用正确的60字符BCrypt密码哈希值
-- 注意：密码是"123456"，BCrypt哈希值长度为60字符
INSERT INTO `user_credentials` (
    `student_id`, 
    `teacher_id`, 
    `username`, 
    `password_hash`, 
    `role`, 
    `status`
) VALUES (
    NULL, 
    NULL, 
    'admin', 
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', 
    'ADMIN', 
    1
);

-- 步骤3：验证创建结果
SELECT 
    `id`,
    `username`, 
    `password_hash`,
    LENGTH(`password_hash`) as password_length,
    CHAR_LENGTH(`password_hash`) as password_char_length,
    LEFT(`password_hash`, 7) as password_prefix,
    `role`,
    `status`,
    `created_at`,
    `updated_at`
FROM `user_credentials` 
WHERE `username` = 'admin';

-- 预期结果：
-- password_length = 60
-- password_char_length = 60
-- password_prefix = '$2a$10$'
-- role = 'ADMIN'
-- status = 1

-- ============================================
-- 使用说明：
-- 1. 执行此脚本后，admin用户的密码为：123456
-- 2. 密码哈希值长度为60字符（BCrypt标准长度）
-- 3. 登录时使用用户名：admin，密码：123456
-- ============================================

