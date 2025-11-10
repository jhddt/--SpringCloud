-- 修复admin用户的密码哈希值
-- 问题：密码哈希值长度为61，应该是60
-- 解决方案：删除并重新插入，确保没有隐藏字符

USE `user_service_db`;

-- 步骤1：删除现有的admin用户
DELETE FROM `user_credentials` WHERE `username` = 'admin';

-- 步骤2：重新插入admin用户，使用正确的60字符BCrypt密码
-- 注意：确保密码哈希值严格为60字符，没有额外的空格或换行符
INSERT INTO `user_credentials` (`student_id`, `teacher_id`, `username`, `password_hash`, `role`, `status`) 
VALUES (NULL, NULL, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', 'ADMIN', 1);

-- 步骤3：验证结果（必须显示60）
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

