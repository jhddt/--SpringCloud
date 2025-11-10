-- ============================================
-- 检查和管理员账号
-- ============================================

USE user_service_db;

-- 1. 检查是否存在管理员账号
SELECT 
    id,
    username,
    role,
    status,
    created_at,
    student_id,
    teacher_id
FROM user_credentials 
WHERE role = 'ADMIN';

-- 2. 如果不存在管理员账号，执行以下SQL创建（取消注释即可执行）
-- INSERT INTO `user_credentials` (`student_id`, `teacher_id`, `username`, `password_hash`, `role`, `status`) 
-- VALUES (NULL, NULL, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', 'ADMIN', 1);

-- 3. 管理员账号信息
-- 用户名：admin
-- 密码：123456
-- 角色：ADMIN
-- 状态：1（启用）

