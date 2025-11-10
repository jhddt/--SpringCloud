-- 修复密码长度问题
-- 问题：密码长度为 61，应该是 60（BCrypt 标准长度）
-- 原因：可能包含额外的空格或换行符

USE `education_management`;

-- 方法1：清理并更新密码（去除首尾空格和换行符）
UPDATE `sys_user` 
SET `password` = TRIM(REPLACE(REPLACE(`password`, '\n', ''), '\r', ''))
WHERE `username` = 'admin';

-- 方法2：如果方法1不行，直接设置为正确的 BCrypt 值
-- UPDATE `sys_user` 
-- SET `password` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO'
-- WHERE `username` = 'admin';

-- 验证修复结果
SELECT 
    `id`, 
    `username`, 
    `password`,
    LENGTH(`password`) as password_length,
    LEFT(`password`, 6) as password_prefix,
    RIGHT(`password`, 1) as password_last_char,
    HEX(RIGHT(`password`, 1)) as password_last_char_hex,
    `role`,
    `status`
FROM `sys_user` 
WHERE `username` = 'admin';

-- 预期结果：
-- password_length 应该是 60（不是 61）
-- password_prefix 应该是 '$2a$10'
-- password_last_char 应该是字母或数字（不是换行符或空格）

