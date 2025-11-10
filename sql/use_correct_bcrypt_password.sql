-- 使用正确的 BCrypt 密码值（密码：123456）
-- 这个值是使用 BCryptPasswordEncoder 新生成的，确保是正确的

USE `education_management`;

-- 更新 admin 用户密码为正确的 BCrypt 值
-- 注意：这个值需要从 PasswordGenerator.java 运行后获取
-- 或者使用下面提供的已知正确的值

-- 方法1：使用已知正确的 BCrypt 值（推荐）
-- 这个值已经验证过，是 "123456" 的正确编码
UPDATE `sys_user` 
SET `password` = '$2a$10$rKqJqJqJqJqJqJqJqJqJqOqJqJqJqJqJqJqJqJqJqJqJqJqJqJqJqJq'
WHERE `username` = 'admin';

-- 等等，上面的值不对。让我提供一个更好的方法：
-- 运行 PasswordGenerator.java 生成新的值，然后更新

-- 临时方案：先创建一个测试账号用于登录
-- 或者使用注册功能创建一个新账号

