-- 检查并修复密码问题
-- 问题：密码长度为 61，但应该是 60

USE `education_management`;

-- 步骤1：检查表结构和字符集
SHOW CREATE TABLE `sys_user`;

-- 步骤2：检查当前密码的详细信息
SELECT 
    `username`,
    `password`,
    LENGTH(`password`) as pwd_length,
    CHAR_LENGTH(`password`) as pwd_char_length,
    HEX(`password`) as pwd_hex,
    LENGTH(HEX(`password`)) as pwd_hex_length
FROM `sys_user`
WHERE `username` = 'admin';

-- 步骤3：如果 HEX 长度是 122（60*2），说明密码本身是正确的
-- 问题可能是数据库字段的字符集或编码导致的

-- 步骤4：尝试使用 BINARY 类型强制更新
-- 先检查字段类型
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    CHARACTER_SET_NAME,
    COLLATION_NAME,
    COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'education_management'
  AND TABLE_NAME = 'sys_user'
  AND COLUMN_NAME = 'password';

-- 步骤5：如果字段是 VARCHAR，尝试使用 CAST 或 CONVERT
-- 但首先，让我们尝试一个不同的方法：使用十六进制字符串
-- BCrypt 密码的十六进制表示（60字符 = 120个十六进制字符）
-- 但这个方法太复杂，不如直接检查是否是字符集问题

-- 步骤6：最简单的方法 - 检查密码值是否真的包含61个字符
-- 如果 HEX 显示是 120 个字符（60*2），说明密码本身是正确的
-- 问题可能是 MySQL 的 LENGTH 函数在某些字符集下的计算问题

-- 步骤7：尝试使用 SUBSTRING 提取前60个字符
UPDATE `sys_user`
SET `password` = SUBSTRING(`password`, 1, 60)
WHERE `username` = 'admin';

-- 步骤8：验证
SELECT 
    `username`,
    LENGTH(`password`) as pwd_length,
    CHAR_LENGTH(`password`) as pwd_char_length,
    LEFT(`password`, 6) as pwd_prefix,
    RIGHT(`password`, 1) as pwd_last_char
FROM `sys_user`
WHERE `username` = 'admin';

