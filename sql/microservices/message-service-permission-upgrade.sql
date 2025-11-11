-- ============================================
-- message-service 权限升级脚本
-- 按照学习通消息权限划分机制重构数据库表结构
-- ============================================

USE `message_service_db`;

-- 1. 添加新字段到messages表
ALTER TABLE `messages` 
ADD COLUMN `content_type` VARCHAR(20) DEFAULT 'TEXT' COMMENT '消息内容类型：TEXT（文本）, IMAGE（图片）, FILE（文件）' AFTER `message_type`,
ADD COLUMN `scope_type` VARCHAR(20) DEFAULT 'PRIVATE' COMMENT '消息范围类型：COURSE（课程）, GROUP（群组）, GLOBAL（全局）, PRIVATE（私聊）' AFTER `content`,
ADD COLUMN `scope_id` BIGINT DEFAULT NULL COMMENT '所属对象ID（课程ID / 群ID）' AFTER `scope_type`,
ADD COLUMN `role_mask` VARCHAR(255) DEFAULT NULL COMMENT '可见角色标识（JSON数组格式，如：["teacher", "student"]）' AFTER `scope_id`;

-- 2. 更新message_type字段的注释，支持新的消息类型
ALTER TABLE `messages` 
MODIFY COLUMN `message_type` VARCHAR(50) DEFAULT 'INSTANT_MESSAGE' COMMENT '消息类型：INSTANT_MESSAGE（即时消息）, SYSTEM_NOTICE（系统通知）, INTERACTION_REMINDER（互动提醒）, PLATFORM_ANNOUNCEMENT（平台公告）';

-- 3. 添加索引以优化查询性能
ALTER TABLE `messages` 
ADD INDEX `idx_scope_type_id` (`scope_type`, `scope_id`),
ADD INDEX `idx_message_type` (`message_type`),
ADD INDEX `idx_role_mask` (`role_mask`(100));

-- 4. 更新现有数据的默认值
-- 将旧数据迁移为新格式
UPDATE `messages` 
SET 
    `message_type` = 'INSTANT_MESSAGE',
    `content_type` = 'TEXT',
    `scope_type` = 'PRIVATE'
WHERE `message_type` IS NULL OR `message_type` = '';

-- 5. 更新sender_type和receiver_type字段，支持SYSTEM角色
ALTER TABLE `messages` 
MODIFY COLUMN `sender_type` VARCHAR(20) DEFAULT NULL COMMENT '发送者类型：STUDENT, TEACHER, ADMIN, SYSTEM',
MODIFY COLUMN `receiver_type` VARCHAR(20) DEFAULT NULL COMMENT '接收者类型：STUDENT, TEACHER, ADMIN, GROUP';

-- 6. 验证数据完整性
SELECT 
    COUNT(*) as total_messages,
    COUNT(CASE WHEN scope_type IS NULL THEN 1 END) as null_scope_type,
    COUNT(CASE WHEN message_type IS NULL THEN 1 END) as null_message_type
FROM `messages`;

-- 7. 创建消息权限视图（可选，用于统计分析）
CREATE OR REPLACE VIEW `v_message_permissions` AS
SELECT 
    m.message_id,
    m.message_type,
    m.scope_type,
    m.scope_id,
    m.role_mask,
    m.sender_id,
    m.sender_type,
    m.receiver_id,
    m.receiver_type,
    m.status,
    m.created_at
FROM `messages` m;

-- 输出升级完成信息
SELECT '消息服务权限升级完成！' AS Message;

