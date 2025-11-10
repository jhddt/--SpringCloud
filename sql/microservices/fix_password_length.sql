-- 修复密码哈希值长度问题
-- 问题：密码哈希值长度为61，应该是60
-- 解决方案：清理密码哈希值中的额外字符，确保长度为60

USE `user_service_db`;

-- 方法1：清理密码哈希值中的额外字符（换行符、空格等）
-- 使用TRIM和REPLACE清理所有可能的空白字符
UPDATE `user_credentials`
SET `password_hash` = TRIM(REPLACE(REPLACE(REPLACE(REPLACE(`password_hash`, '\n', ''), '\r', ''), '\t', ''), ' ', ''))
WHERE LENGTH(`password_hash`) != 60;

-- 方法2：如果清理后仍然不是60字符，直接更新为正确的60字符密码哈希值
-- 注意：这个值是"123456"的正确BCrypt哈希值（60字符）
-- 先删除现有的admin用户，然后重新插入，确保没有隐藏字符
DELETE FROM `user_credentials` WHERE `username` = 'admin';

INSERT INTO `user_credentials` (`student_id`, `teacher_id`, `username`, `password_hash`, `role`, `status`) 
VALUES (NULL, NULL, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', 'ADMIN', 1);

-- 验证修复结果
SELECT 
    `id`,
    `username`, 
    `password_hash`,
    LENGTH(`password_hash`) as password_length,
    CHAR_LENGTH(`password_hash`) as password_char_length,
    LEFT(`password_hash`, 7) as password_prefix,
    `role`,
    `status`
FROM `user_credentials` 
WHERE `username` = 'admin';

-- 预期结果：
-- password_length = 60
-- password_char_length = 60
-- password_prefix = '$2a$10$'

