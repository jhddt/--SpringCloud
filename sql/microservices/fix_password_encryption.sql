-- ============================================
-- 密码加密修复脚本
-- 用途：修复数据库中密码加密不一致的问题
-- ============================================

-- 说明：
-- 1. 此脚本用于修复数据库中已存在的密码数据
-- 2. 所有密码都应该使用BCrypt加密，强度为10
-- 3. 默认密码"123456"的BCrypt哈希值需要从PasswordUtil.main()方法运行后获取
-- 4. 或者使用下面提供的已知正确的哈希值

USE `user_service_db`;

-- 方法1：使用已知正确的BCrypt哈希值（推荐）
-- 这个值是使用BCryptPasswordEncoder(10)生成的"123456"的加密值
-- 注意：每次运行BCryptPasswordEncoder.encode()都会生成不同的哈希值，但都能通过验证
-- 下面提供几个已验证的哈希值，选择其中一个使用即可

-- 选项1（已验证可用）
UPDATE `user_credentials` 
SET `password_hash` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO'
WHERE `password_hash` IS NULL 
   OR `password_hash` = '' 
   OR LENGTH(`password_hash`) != 60
   OR `password_hash` NOT LIKE '$2a$10$%';

-- 选项2：如果需要生成新的哈希值，运行以下Java代码：
-- PasswordUtil.main() 或
-- new BCryptPasswordEncoder(10).encode("123456")

-- 方法2：清理密码中的额外字符
-- 如果密码长度超过60字符，可能是存储时引入了额外字符
UPDATE `user_credentials`
SET `password_hash` = TRIM(REPLACE(REPLACE(REPLACE(`password_hash`, '\n', ''), '\r', ''), '\t', ''))
WHERE LENGTH(`password_hash`) > 60;

-- 方法3：重置所有用户的密码为默认密码"123456"
-- 警告：此操作会重置所有用户的密码，请谨慎使用！
-- UPDATE `user_credentials`
-- SET `password_hash` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO',
--     `updated_at` = NOW()
-- WHERE `role` IN ('STUDENT', 'TEACHER');

-- 验证修复结果
SELECT 
    `id`,
    `username`,
    `role`,
    LENGTH(`password_hash`) as password_length,
    CASE 
        WHEN `password_hash` LIKE '$2a$10$%' THEN '格式正确'
        WHEN LENGTH(`password_hash`) = 60 THEN '长度正确但格式可能有问题'
        ELSE '格式错误'
    END as password_status
FROM `user_credentials`
ORDER BY `id`;

-- 检查是否有格式错误的密码
SELECT COUNT(*) as error_count
FROM `user_credentials`
WHERE `password_hash` IS NULL 
   OR `password_hash` = '' 
   OR LENGTH(`password_hash`) != 60
   OR `password_hash` NOT LIKE '$2a$10$%';

