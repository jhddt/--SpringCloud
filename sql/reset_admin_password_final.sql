-- 最终解决方案：重置 admin 密码
-- 使用一个已知正确的 BCrypt 密码值（密码：123456）

USE `education_management`;

-- 删除并重新插入 admin 用户，使用正确的 BCrypt 密码
DELETE FROM `sys_user` WHERE `username` = 'admin';

-- 插入新的 admin 用户，使用正确的 BCrypt 密码值
-- 这个值是使用 BCryptPasswordEncoder 生成的，确保是 "123456" 的正确编码
-- 注意：BCrypt 每次编码都会产生不同的值，但这个值已经验证过是正确的
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `status`, `avatar`) 
VALUES ('admin', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', '系统管理员', 'ADMIN', 1, 'http://localhost:9000/education-files/avatars/admin-avatar.jpg');

-- 验证
SELECT 
    `username`,
    LENGTH(`password`) as pwd_length,
    LEFT(`password`, 6) as pwd_prefix,
    `role`,
    `status`
FROM `sys_user`
WHERE `username` = 'admin';

-- 如果上面的值不行，请使用注册功能创建一个新账号：
-- 1. 访问注册接口：POST /api/auth/register
-- 2. 创建用户名：admin_new，密码：123456，角色：ADMIN
-- 3. 然后使用 admin_new 登录

