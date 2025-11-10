-- ============================================
-- user-service 数据库初始化脚本
-- 数据库名：user_service_db
-- 功能：学生信息管理
-- ============================================

CREATE DATABASE IF NOT EXISTS `user_service_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `user_service_db`;

-- 学生表（students）
CREATE TABLE IF NOT EXISTS `students` (
  `student_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '学生ID',
  `name` VARCHAR(50) NOT NULL COMMENT '姓名',
  `gender` VARCHAR(10) DEFAULT NULL COMMENT '性别：MALE, FEMALE',
  `date_of_birth` DATE DEFAULT NULL COMMENT '出生日期',
  `major` VARCHAR(100) DEFAULT NULL COMMENT '专业',
  `grade` VARCHAR(20) DEFAULT NULL COMMENT '年级',
  `class_name` VARCHAR(50) DEFAULT NULL COMMENT '班级',
  `contact_info` VARCHAR(255) DEFAULT NULL COMMENT '联系方式（JSON格式）',
  `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像图片路径',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`student_id`),
  KEY `idx_status` (`status`),
  KEY `idx_major` (`major`),
  KEY `idx_grade` (`grade`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生信息表';

-- 登录信息表（user_credentials）- 统一管理所有用户的登录凭证
CREATE TABLE IF NOT EXISTS `user_credentials` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `student_id` BIGINT DEFAULT NULL COMMENT '学生ID（FK，当role=STUDENT时）',
  `teacher_id` BIGINT DEFAULT NULL COMMENT '教师ID（来自teacher-service，当role=TEACHER时）',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名（学号/工号/管理员账号）',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希值（BCrypt）',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `role` VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT '角色：ADMIN, TEACHER, STUDENT',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_student_id` (`student_id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_user_credentials_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录信息表（统一管理所有用户：管理员、教师、学生）';

-- 初始化管理员账号（student_id和teacher_id都为NULL）
-- 注意：管理员账号不关联students或teachers表
INSERT INTO `user_credentials` (`student_id`, `teacher_id`, `username`, `password_hash`, `role`, `status`) 
VALUES (NULL, NULL, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', 'ADMIN', 1);

-- 初始化测试学生数据
INSERT INTO `students` (`name`, `gender`, `date_of_birth`, `major`, `grade`, `class_name`, `contact_info`, `avatar_url`, `status`) VALUES
('李同学', 'MALE', '2000-01-15', '计算机科学与技术', '2024', '计科2024-1班', '{"phone":"13900139001","email":"lixue@edu.cn"}', 'http://localhost:9000/education-files/avatars/student1-avatar.jpg', 1),
('王同学', 'FEMALE', '2000-02-20', '计算机科学与技术', '2024', '计科2024-1班', '{"phone":"13900139002","email":"wangxue@edu.cn"}', 'http://localhost:9000/education-files/avatars/student2-avatar.jpg', 1),
('张同学', 'MALE', '2000-03-10', '计算机科学与技术', '2024', '计科2024-2班', '{"phone":"13900139003","email":"zhangxue@edu.cn"}', 'http://localhost:9000/education-files/avatars/student3-avatar.jpg', 1),
('刘同学', 'FEMALE', '2000-04-05', '数学与应用数学', '2024', '数学2024-1班', '{"phone":"13900139004","email":"liuxue@edu.cn"}', 'http://localhost:9000/education-files/avatars/student4-avatar.jpg', 1),
('陈同学', 'MALE', '2000-05-12', '数学与应用数学', '2024', '数学2024-1班', '{"phone":"13900139005","email":"chenxue@edu.cn"}', 'http://localhost:9000/education-files/avatars/student5-avatar.jpg', 1);

-- 初始化学生登录信息（密码均为123456的BCrypt加密值）
INSERT INTO `user_credentials` (`student_id`, `username`, `password_hash`, `email`, `phone`, `role`, `status`) VALUES
(1, 'S2024001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', 'lixue@edu.cn', '13900139001', 'STUDENT', 1),
(2, 'S2024002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', 'wangxue@edu.cn', '13900139002', 'STUDENT', 1),
(3, 'S2024003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', 'zhangxue@edu.cn', '13900139003', 'STUDENT', 1),
(4, 'S2024004', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', 'liuxue@edu.cn', '13900139004', 'STUDENT', 1),
(5, 'S2024005', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', 'chenxue@edu.cn', '13900139005', 'STUDENT', 1);

