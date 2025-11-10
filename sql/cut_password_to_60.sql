-- 截取密码为 60 字符
-- 问题：密码长度为 61，但最后一个字符是正确的 'O'
-- 可能是 MySQL LENGTH 函数在 utf8mb4 字符集下的计算问题

USE `education_management`;

-- 方法：直接截取前 60 个字符
UPDATE `sys_user`
SET `password` = SUBSTRING(`password`, 1, 60)
WHERE `username` = 'admin';

-- 验证
SELECT 
    `username`,
    `password`,
    LENGTH(`password`) as pwd_length,
    CHAR_LENGTH(`password`) as pwd_char_length,
    LEFT(`password`, 6) as pwd_prefix,
    RIGHT(`password`, 1) as pwd_last_char,
    ASCII(RIGHT(`password`, 1)) as pwd_last_char_ascii
FROM `sys_user`
WHERE `username` = 'admin';

-- 如果截取后长度仍然是 61，说明问题在数据库层面
-- 可以尝试使用 BINARY 类型或者检查字符集

