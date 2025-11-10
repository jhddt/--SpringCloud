-- ============================================
-- enrollment-service 数据库初始化脚本
-- 数据库名：enrollment_service_db
-- 功能：选课管理
-- ============================================

CREATE DATABASE IF NOT EXISTS `enrollment_service_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `enrollment_service_db`;

-- 选课表（enrollments）
CREATE TABLE IF NOT EXISTS `enrollments` (
  `enrollment_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '选课ID',
  `student_id` BIGINT NOT NULL COMMENT '学生ID（来自user-service）',
  `course_id` BIGINT NOT NULL COMMENT '课程ID（来自course-service）',
  `enrollment_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
  `status` TINYINT DEFAULT 0 COMMENT '选课状态：0-已选，1-已退',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`enrollment_id`),
  UNIQUE KEY `uk_student_course` (`student_id`, `course_id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_status` (`status`),
  KEY `idx_enrollment_time` (`enrollment_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='选课记录表';

-- 初始化测试选课数据
INSERT INTO `enrollments` (`student_id`, `course_id`, `enrollment_time`, `status`) VALUES
(1, 1, '2024-09-05 10:00:00', 0),
(1, 2, '2024-09-05 10:05:00', 0),
(1, 4, '2024-09-05 10:10:00', 0),
(2, 1, '2024-09-05 11:00:00', 0),
(2, 3, '2024-09-05 11:05:00', 0),
(2, 7, '2024-09-05 11:10:00', 0),
(3, 1, '2024-09-05 12:00:00', 0),
(3, 5, '2024-09-05 12:05:00', 0),
(3, 11, '2024-09-05 12:10:00', 0),
(4, 4, '2024-09-05 13:00:00', 0),
(4, 5, '2024-09-05 13:05:00', 0),
(4, 14, '2024-09-05 13:10:00', 0);

