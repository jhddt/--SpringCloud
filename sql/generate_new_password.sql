-- 生成新的 BCrypt 密码
-- 注意：这个 SQL 文件只是说明，实际需要运行 Java 代码生成

-- 方法1：使用注册功能创建新账号（推荐）
-- 通过 API 注册一个新账号，密码会自动编码

-- 方法2：运行 PasswordGenerator.java 生成新的 BCrypt 密码
-- 然后执行以下 SQL 更新：
-- UPDATE `sys_user` 
-- SET `password` = '新生成的BCrypt值'
-- WHERE `username` = 'admin';

-- 方法3：临时方案 - 创建一个测试管理员账号
-- INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `status`) 
-- VALUES ('admin2', '$2a$10$新生成的BCrypt值', '测试管理员', 'ADMIN', 1);

