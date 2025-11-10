-- ============================================
-- message-service 数据库初始化脚本
-- 数据库名：message_service_db
-- 功能：消息通信（WebSocket）
-- ============================================

CREATE DATABASE IF NOT EXISTS `message_service_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `message_service_db`;

-- 消息表（messages）
CREATE TABLE IF NOT EXISTS `messages` (
  `message_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `sender_id` BIGINT NOT NULL COMMENT '发送者ID（学生ID或教师ID，来自对应服务）',
  `sender_type` VARCHAR(20) DEFAULT NULL COMMENT '发送者类型：STUDENT, TEACHER, ADMIN',
  `receiver_id` BIGINT NOT NULL COMMENT '接收者ID（学生ID或教师ID，来自对应服务）',
  `receiver_type` VARCHAR(20) DEFAULT NULL COMMENT '接收者类型：STUDENT, TEACHER, ADMIN',
  `message_type` VARCHAR(20) DEFAULT 'TEXT' COMMENT '消息类型：TEXT（文本）, IMAGE（图片）, FILE（文件）, NOTIFICATION（通知）',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0-未读，1-已读',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`message_id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- WebSocket会话表（sessions）
-- 注意：此表主要用于记录在线用户，实际会话信息可能更多存储在Redis中
CREATE TABLE IF NOT EXISTS `sessions` (
  `session_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID（学生ID或教师ID，来自对应服务）',
  `user_type` VARCHAR(20) DEFAULT NULL COMMENT '用户类型：STUDENT, TEACHER, ADMIN',
  `socket_id` VARCHAR(255) DEFAULT NULL COMMENT 'WebSocket连接标识',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-离线，1-在线',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_socket_id` (`socket_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WebSocket会话表';

-- 初始化测试消息数据
INSERT INTO `messages` (`sender_id`, `sender_type`, `receiver_id`, `receiver_type`, `message_type`, `content`, `status`, `created_at`) VALUES
(1, 'TEACHER', 1, 'STUDENT', 'TEXT', '欢迎选课！如有问题请随时联系。', 1, '2024-09-05 10:00:00'),
(1, 'STUDENT', 1, 'TEACHER', 'TEXT', '老师，我想了解一下Java课程的具体安排。', 1, '2024-09-05 10:30:00'),
(1, 'TEACHER', 1, 'STUDENT', 'TEXT', '课程安排已发送到你的邮箱，请查收。', 0, '2024-09-05 11:00:00'),
(1, 'TEACHER', 2, 'STUDENT', 'TEXT', '你的选课申请已通过，请按时上课。', 1, '2024-09-05 15:00:00'),
(2, 'STUDENT', 1, 'TEACHER', 'TEXT', '谢谢老师！', 1, '2024-09-05 15:30:00'),
(2, 'TEACHER', 1, 'STUDENT', 'TEXT', '高等数学课程本周开始，请做好准备。', 1, '2024-09-06 09:00:00'),
(1, 'STUDENT', 2, 'TEACHER', 'TEXT', '好的，老师。', 1, '2024-09-06 09:15:00'),
(1, 'TEACHER', 3, 'STUDENT', 'TEXT', '数据结构课程需要提前预习第一章内容。', 0, '2024-09-06 10:00:00'),
(4, 'TEACHER', 1, 'STUDENT', 'TEXT', '英语口语课程需要准备自我介绍。', 1, '2024-09-06 11:00:00'),
(5, 'TEACHER', 2, 'STUDENT', 'TEXT', '经济学课程推荐阅读《经济学原理》。', 1, '2024-09-06 14:00:00');

