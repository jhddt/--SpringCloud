-- 强制修复密码长度问题
-- 直接设置为正确的 60 字符 BCrypt 值（密码：123456）

USE `education_management`;

-- 方法1：直接设置为正确的 BCrypt 值（60字符）
UPDATE `sys_user` 
SET `password` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO'
WHERE `username` = 'admin';

-- 方法2：如果方法1不行，先删除再插入（确保没有隐藏字符）
-- DELETE FROM `sys_user` WHERE `username` = 'admin';
-- INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `status`) 
-- VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', '系统管理员', 'ADMIN', 1);

-- 验证修复结果（必须显示 60）
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
-- password_length 和 password_char_length 都应该是 60
-- password_prefix 应该是 '$2a$10'
-- password_last_char 应该是 'O'（字母O）
-- password_last_char_ascii 应该是 79（字母O的ASCII码）

