-- ============================================
-- teacher-service 数据库初始化脚本
-- 数据库名：teacher_service_db
-- 功能：教师信息管理
-- ============================================

CREATE DATABASE IF NOT EXISTS `teacher_service_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `teacher_service_db`;

-- 教师表（teachers）
CREATE TABLE IF NOT EXISTS `teachers` (
  `teacher_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '教师ID',
  `name` VARCHAR(50) NOT NULL COMMENT '姓名',
  `gender` VARCHAR(10) DEFAULT NULL COMMENT '性别：MALE, FEMALE',
  `department` VARCHAR(100) DEFAULT NULL COMMENT '部门/学院',
  `title` VARCHAR(50) DEFAULT NULL COMMENT '职称',
  `contact_info` VARCHAR(255) DEFAULT NULL COMMENT '联系方式（JSON格式）',
  `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像图片路径',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`teacher_id`),
  KEY `idx_department` (`department`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师信息表';

-- 教师课程关联表（teacher_courses）
CREATE TABLE IF NOT EXISTS `teacher_courses` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `teacher_id` BIGINT NOT NULL COMMENT '教师ID（FK）',
  `course_id` BIGINT NOT NULL COMMENT '课程ID（FK，来自course-service）',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_course` (`teacher_id`, `course_id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_course_id` (`course_id`),
  CONSTRAINT `fk_teacher_courses_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`teacher_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师课程关联表';

-- 初始化测试教师数据
INSERT INTO `teachers` (`name`, `gender`, `department`, `title`, `contact_info`, `avatar_url`, `status`) VALUES
('张明', 'MALE', '计算机科学学院', '教授', '{"phone":"13800138001","email":"zhangming@edu.cn"}', 'http://localhost:9000/education-files/avatars/teacher1-avatar.jpg', 1),
('王芳', 'FEMALE', '数学与统计学院', '副教授', '{"phone":"13800138002","email":"wangfang@edu.cn"}', 'http://localhost:9000/education-files/avatars/teacher2-avatar.jpg', 1),
('李强', 'MALE', '物理与电子工程学院', '教授', '{"phone":"13800138003","email":"liqiang@edu.cn"}', 'http://localhost:9000/education-files/avatars/teacher3-avatar.jpg', 1),
('刘静', 'FEMALE', '外国语学院', '讲师', '{"phone":"13800138004","email":"liujing@edu.cn"}', 'http://localhost:9000/education-files/avatars/teacher4-avatar.jpg', 1),
('陈伟', 'MALE', '经济管理学院', '副教授', '{"phone":"13800138005","email":"chenwei@edu.cn"}', 'http://localhost:9000/education-files/avatars/teacher5-avatar.jpg', 1);

-- 注意：teacher_courses 表的数据需要在课程创建后通过服务间调用添加

