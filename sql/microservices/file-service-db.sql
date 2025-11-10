-- ============================================
-- file-service 数据库初始化脚本
-- 数据库名：file_service_db
-- 功能：文件上传管理（MinIO）
-- ============================================

CREATE DATABASE IF NOT EXISTS `file_service_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `file_service_db`;

-- 文件表（files）
CREATE TABLE IF NOT EXISTS `files` (
  `file_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `file_type` VARCHAR(50) NOT NULL COMMENT '文件类型：AVATAR（头像）, COURSE_COVER（课程封面）, DOCUMENT（文档）, OTHER（其他）',
  `file_url` VARCHAR(500) NOT NULL COMMENT '文件URL（MinIO存储路径）',
  `file_name` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
  `file_size` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
  `uploaded_by` BIGINT DEFAULT NULL COMMENT '上传者ID（学生ID或教师ID，来自对应服务）',
  `uploaded_by_type` VARCHAR(20) DEFAULT NULL COMMENT '上传者类型：STUDENT, TEACHER, ADMIN',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`file_id`),
  KEY `idx_file_type` (`file_type`),
  KEY `idx_uploaded_by` (`uploaded_by`),
  KEY `idx_uploaded_by_type` (`uploaded_by_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件信息表';

-- 初始化测试文件数据
INSERT INTO `files` (`file_type`, `file_url`, `file_name`, `file_size`, `uploaded_by`, `uploaded_by_type`) VALUES
('AVATAR', 'http://localhost:9000/education-files/avatars/admin-avatar.jpg', 'admin-avatar.jpg', 102400, 0, 'ADMIN'),
('AVATAR', 'http://localhost:9000/education-files/avatars/teacher1-avatar.jpg', 'teacher1-avatar.jpg', 102400, 1, 'TEACHER'),
('AVATAR', 'http://localhost:9000/education-files/avatars/student1-avatar.jpg', 'student1-avatar.jpg', 102400, 1, 'STUDENT'),
('COURSE_COVER', 'http://localhost:9000/education-files/course-covers/java-cover.jpg', 'java-cover.jpg', 204800, 1, 'TEACHER'),
('COURSE_COVER', 'http://localhost:9000/education-files/course-covers/datastructure-cover.jpg', 'datastructure-cover.jpg', 204800, 1, 'TEACHER');

