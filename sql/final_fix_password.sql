-- 最终修复密码脚本
-- 问题：密码长度为 61，应该是 60
-- 解决方案：删除并重新插入，确保没有隐藏字符

USE `education_management`;

-- 步骤1：删除现有的 admin 用户
DELETE FROM `sys_user` WHERE `username` = 'admin';

-- 步骤2：重新插入 admin 用户，使用正确的 60 字符 BCrypt 密码
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `status`, `avatar`) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', '系统管理员', 'ADMIN', 1, 'http://localhost:9000/education-files/avatars/admin-avatar.jpg');

-- 步骤3：验证结果（必须显示 60）
SELECT 
    `id`,
    `username`, 
    `password`,
    LENGTH(`password`) as password_length,
    CHAR_LENGTH(`password`) as password_char_length,
    LEFT(`password`, 6) as password_prefix,
    RIGHT(`password`, 1) as password_last_char,
    ASCII(RIGHT(`password`, 1)) as password_last_char_ascii,
    `role`,
    `status`
FROM `sys_user` 
WHERE `username` = 'admin';

-- 预期结果：
-- password_length = 60
-- password_char_length = 60
-- password_prefix = '$2a$10'
-- password_last_char = 'O'
-- password_last_char_ascii = 79

