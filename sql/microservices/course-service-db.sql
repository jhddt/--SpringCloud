-- ============================================
-- course-service 数据库初始化脚本
-- 数据库名：course_service_db
-- 功能：课程信息管理
-- ============================================

CREATE DATABASE IF NOT EXISTS `course_service_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `course_service_db`;

-- 课程表（courses）
CREATE TABLE IF NOT EXISTS `courses` (
  `course_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '课程ID',
  `course_name` VARCHAR(100) NOT NULL COMMENT '课程名称',
  `course_code` VARCHAR(50) NOT NULL COMMENT '课程代码',
  `course_description` TEXT COMMENT '课程描述',
  `credit` DECIMAL(3,1) DEFAULT 0.0 COMMENT '学分',
  `teacher_id` BIGINT DEFAULT NULL COMMENT '授课教师ID（来自teacher-service）',
  `teacher_name` VARCHAR(50) DEFAULT NULL COMMENT '教师姓名（冗余字段，便于查询）',
  `department` VARCHAR(100) DEFAULT NULL COMMENT '开课院系',
  `total_capacity` INT DEFAULT 0 COMMENT '总容量',
  `selected_count` INT DEFAULT 0 COMMENT '已选人数',
  `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面图片URL',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-未开放，1-开放选课，2-已结束',
  `start_time` DATETIME DEFAULT NULL COMMENT '选课开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '选课结束时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`course_id`),
  UNIQUE KEY `uk_course_code` (`course_code`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_status` (`status`),
  KEY `idx_department` (`department`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程信息表';

-- 课程时间表（course_schedule）
CREATE TABLE IF NOT EXISTS `course_schedule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `course_id` BIGINT NOT NULL COMMENT '课程ID（FK）',
  `day_of_week` TINYINT NOT NULL COMMENT '星期几：1-周一，2-周二，...，7-周日',
  `start_time` TIME NOT NULL COMMENT '开始时间',
  `end_time` TIME NOT NULL COMMENT '结束时间',
  `classroom` VARCHAR(50) DEFAULT NULL COMMENT '教室',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`),
  CONSTRAINT `fk_course_schedule_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`course_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程时间表';

-- 初始化测试课程数据
INSERT INTO `courses` (`course_code`, `course_name`, `teacher_id`, `teacher_name`, `course_description`, `cover_image`, `credit`, `total_capacity`, `selected_count`, `status`, `start_time`, `end_time`, `department`) VALUES
('CS101', 'Java程序设计', 1, '张明', '本课程主要介绍Java语言的基础语法、面向对象编程、集合框架、IO流、多线程等内容，通过理论学习和实践操作，使学生掌握Java编程的基本技能。', 'http://localhost:9000/education-files/course-covers/java-cover.jpg', 4.0, 50, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59', '计算机科学学院'),
('CS102', '数据结构与算法', 1, '张明', '本课程系统地介绍各种数据结构（线性表、栈、队列、树、图等）和常用算法（排序、查找、图算法等），培养学生分析和解决问题的能力。', 'http://localhost:9000/education-files/course-covers/datastructure-cover.jpg', 4.0, 45, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59', '计算机科学学院'),
('CS103', '数据库系统原理', 1, '张明', '本课程介绍数据库系统的基本概念、关系数据模型、SQL语言、数据库设计、事务处理等内容，使学生掌握数据库系统的设计和管理能力。', 'http://localhost:9000/education-files/course-covers/database-cover.jpg', 3.5, 40, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59', '计算机科学学院'),
('MATH201', '高等数学A', 2, '王芳', '本课程是理工科学生的重要基础课程，内容包括函数、极限、连续、导数、微分、积分、级数等，为后续专业课程打下坚实的数学基础。', 'http://localhost:9000/education-files/course-covers/calculus-cover.jpg', 5.0, 60, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59', '数学与统计学院'),
('MATH202', '线性代数', 2, '王芳', '本课程介绍向量空间、矩阵、行列式、线性方程组、特征值与特征向量等内容，是计算机科学、物理学等专业的重要数学工具。', 'http://localhost:9000/education-files/course-covers/linear-algebra-cover.jpg', 3.0, 55, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59', '数学与统计学院'),
('PHY301', '大学物理', 3, '李强', '本课程系统地介绍力学、热学、电磁学、光学等物理学基础知识，培养学生的物理思维和实验能力。', 'http://localhost:9000/education-files/course-covers/physics-cover.jpg', 4.0, 50, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59', '物理与电子工程学院'),
('ENG401', '大学英语', 4, '刘静', '本课程旨在提高学生的英语综合应用能力，包括听、说、读、写、译等方面，为学生的国际化发展打下基础。', 'http://localhost:9000/education-files/course-covers/english-cover.jpg', 3.0, 80, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59', '外国语学院'),
('ENG402', '英语口语', 4, '刘静', '本课程通过情景对话、角色扮演、演讲等方式，提高学生的英语口语表达能力和跨文化交际能力。', 'http://localhost:9000/education-files/course-covers/english-speaking-cover.jpg', 2.0, 30, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59', '外国语学院'),
('ECO501', '微观经济学', 5, '陈伟', '本课程介绍市场机制、消费者行为、生产者行为、市场结构等微观经济学基本理论，帮助学生理解市场经济运行规律。', 'http://localhost:9000/education-files/course-covers/microeconomics-cover.jpg', 3.5, 45, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59', '经济管理学院'),
('ECO502', '宏观经济学', 5, '陈伟', '本课程介绍国民收入、经济增长、通货膨胀、失业、财政政策、货币政策等宏观经济学内容，帮助学生理解宏观经济运行。', 'http://localhost:9000/education-files/course-covers/macroeconomics-cover.jpg', 3.5, 45, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59', '经济管理学院');

-- 初始化课程时间表数据
INSERT INTO `course_schedule` (`course_id`, `day_of_week`, `start_time`, `end_time`, `classroom`) VALUES
(1, 1, '08:00:00', '09:40:00', 'A101'),
(1, 3, '08:00:00', '09:40:00', 'A101'),
(2, 2, '10:00:00', '11:40:00', 'A102'),
(2, 4, '10:00:00', '11:40:00', 'A102'),
(3, 1, '14:00:00', '15:40:00', 'A103'),
(3, 3, '14:00:00', '15:40:00', 'A103'),
(4, 1, '08:00:00', '09:40:00', 'B201'),
(4, 3, '08:00:00', '09:40:00', 'B201'),
(4, 5, '08:00:00', '09:40:00', 'B201'),
(5, 2, '10:00:00', '11:40:00', 'B202'),
(5, 4, '10:00:00', '11:40:00', 'B202');

