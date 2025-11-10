-- 更新管理员密码为 123456 的 BCrypt 编码值
-- 使用此脚本更新数据库中 admin 用户的密码

USE `education_management`;

-- 更新 admin 用户密码（密码：123456）
-- BCrypt 编码值：$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO
UPDATE `sys_user` 
SET `password` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO' 
WHERE `username` = 'admin';

-- 验证更新结果
SELECT `id`, `username`, `password`, `role` FROM `sys_user` WHERE `username` = 'admin';

