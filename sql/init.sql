 -- 教务选课系统数据库初始化脚本

CREATE DATABASE IF NOT EXISTS `education_management` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `education_management`;

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `role` VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT '角色：ADMIN, TEACHER, STUDENT',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 学生表
CREATE TABLE IF NOT EXISTS `student` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '学生ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `student_no` VARCHAR(50) NOT NULL COMMENT '学号',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `class_name` VARCHAR(50) DEFAULT NULL COMMENT '班级',
  `major` VARCHAR(100) DEFAULT NULL COMMENT '专业',
  `grade` VARCHAR(20) DEFAULT NULL COMMENT '年级',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_student_no` (`student_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生信息表';

-- 教师表
CREATE TABLE IF NOT EXISTS `teacher` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '教师ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `teacher_no` VARCHAR(50) NOT NULL COMMENT '工号',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `department` VARCHAR(100) DEFAULT NULL COMMENT '部门',
  `title` VARCHAR(50) DEFAULT NULL COMMENT '职称',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_teacher_no` (`teacher_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师信息表';

-- 课程表
CREATE TABLE IF NOT EXISTS `course` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '课程ID',
  `course_code` VARCHAR(50) NOT NULL COMMENT '课程代码',
  `course_name` VARCHAR(100) NOT NULL COMMENT '课程名称',
  `teacher_id` BIGINT DEFAULT NULL COMMENT '授课教师ID',
  `teacher_name` VARCHAR(50) DEFAULT NULL COMMENT '教师姓名',
  `description` TEXT COMMENT '课程描述',
  `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面图片URL',
  `credit` DECIMAL(3,1) DEFAULT 0.0 COMMENT '学分',
  `total_capacity` INT DEFAULT 0 COMMENT '总容量',
  `selected_count` INT DEFAULT 0 COMMENT '已选人数',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-未开放，1-开放选课',
  `start_time` DATETIME DEFAULT NULL COMMENT '选课开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '选课结束时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_code` (`course_code`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程信息表';

-- 选课表
CREATE TABLE IF NOT EXISTS `course_selection` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '选课ID',
  `student_id` BIGINT NOT NULL COMMENT '学生ID',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0-待审核，1-已通过，2-已拒绝',
  `selection_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
  `approve_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_course` (`student_id`, `course_id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='选课记录表';

-- 消息表
CREATE TABLE IF NOT EXISTS `message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
  `receiver_id` BIGINT NOT NULL COMMENT '接收者ID',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `type` VARCHAR(20) DEFAULT 'TEXT' COMMENT '消息类型：TEXT, IMAGE, FILE',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0-未读，1-已读',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 初始化管理员账号
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `status`, `avatar`) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', '系统管理员', 'ADMIN', 1, 'http://localhost:9000/education-files/avatars/admin-avatar.jpg');

-- 初始化测试数据
-- 注意：所有密码均为 123456 的BCrypt加密值：$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO

-- 插入教师用户
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `email`, `phone`, `role`, `status`, `avatar`) VALUES
('teacher1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', '张明', 'zhangming@edu.cn', '13800138001', 'TEACHER', 1, 'http://localhost:9000/education-files/avatars/teacher1-avatar.jpg'),
('teacher2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', '王芳', 'wangfang@edu.cn', '13800138002', 'TEACHER', 1, 'http://localhost:9000/education-files/avatars/teacher2-avatar.jpg'),
('teacher3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', '李强', 'liqiang@edu.cn', '13800138003', 'TEACHER', 1, 'http://localhost:9000/education-files/avatars/teacher3-avatar.jpg'),
('teacher4', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', '刘静', 'liujing@edu.cn', '13800138004', 'TEACHER', 1, 'http://localhost:9000/education-files/avatars/teacher4-avatar.jpg'),
('teacher5', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', '陈伟', 'chenwei@edu.cn', '13800138005', 'TEACHER', 1, 'http://localhost:9000/education-files/avatars/teacher5-avatar.jpg');

-- 插入学生用户
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `email`, `phone`, `role`, `status`, `avatar`) VALUES
('student1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', '李同学', 'lixue@edu.cn', '13900139001', 'STUDENT', 1, 'http://localhost:9000/education-files/avatars/student1-avatar.jpg'),
('student2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', '王同学', 'wangxue@edu.cn', '13900139002', 'STUDENT', 1, 'http://localhost:9000/education-files/avatars/student2-avatar.jpg'),
('student3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', '张同学', 'zhangxue@edu.cn', '13900139003', 'STUDENT', 1, 'http://localhost:9000/education-files/avatars/student3-avatar.jpg'),
('student4', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', '刘同学', 'liuxue@edu.cn', '13900139004', 'STUDENT', 1, 'http://localhost:9000/education-files/avatars/student4-avatar.jpg'),
('student5', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', '陈同学', 'chenxue@edu.cn', '13900139005', 'STUDENT', 1, 'http://localhost:9000/education-files/avatars/student5-avatar.jpg'),
('student6', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', '赵同学', 'zhaoxue@edu.cn', '13900139006', 'STUDENT', 1, 'http://localhost:9000/education-files/avatars/student6-avatar.jpg'),
('student7', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', '孙同学', 'sunxue@edu.cn', '13900139007', 'STUDENT', 1, 'http://localhost:9000/education-files/avatars/student7-avatar.jpg'),
('student8', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', '周同学', 'zhouxue@edu.cn', '13900139008', 'STUDENT', 1, 'http://localhost:9000/education-files/avatars/student8-avatar.jpg'),
('student9', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', '吴同学', 'wuxue@edu.cn', '13900139009', 'STUDENT', 1, 'http://localhost:9000/education-files/avatars/student9-avatar.jpg'),
('student10', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO', '郑同学', 'zhengxue@edu.cn', '13900139010', 'STUDENT', 1, 'http://localhost:9000/education-files/avatars/student10-avatar.jpg');

-- 插入教师信息
INSERT INTO `teacher` (`user_id`, `teacher_no`, `real_name`, `department`, `title`, `phone`, `email`, `avatar`) VALUES
(2, 'T001', '张明', '计算机科学学院', '教授', '13800138001', 'zhangming@edu.cn', 'http://localhost:9000/education-files/avatars/teacher1-avatar.jpg'),
(3, 'T002', '王芳', '数学与统计学院', '副教授', '13800138002', 'wangfang@edu.cn', 'http://localhost:9000/education-files/avatars/teacher2-avatar.jpg'),
(4, 'T003', '李强', '物理与电子工程学院', '教授', '13800138003', 'liqiang@edu.cn', 'http://localhost:9000/education-files/avatars/teacher3-avatar.jpg'),
(5, 'T004', '刘静', '外国语学院', '讲师', '13800138004', 'liujing@edu.cn', 'http://localhost:9000/education-files/avatars/teacher4-avatar.jpg'),
(6, 'T005', '陈伟', '经济管理学院', '副教授', '13800138005', 'chenwei@edu.cn', 'http://localhost:9000/education-files/avatars/teacher5-avatar.jpg');

-- 插入学生信息
INSERT INTO `student` (`user_id`, `student_no`, `real_name`, `class_name`, `major`, `grade`, `phone`, `email`, `avatar`) VALUES
(7, 'S2024001', '李同学', '计科2024-1班', '计算机科学与技术', '2024', '13900139001', 'lixue@edu.cn', 'http://localhost:9000/education-files/avatars/student1-avatar.jpg'),
(8, 'S2024002', '王同学', '计科2024-1班', '计算机科学与技术', '2024', '13900139002', 'wangxue@edu.cn', 'http://localhost:9000/education-files/avatars/student2-avatar.jpg'),
(9, 'S2024003', '张同学', '计科2024-2班', '计算机科学与技术', '2024', '13900139003', 'zhangxue@edu.cn', 'http://localhost:9000/education-files/avatars/student3-avatar.jpg'),
(10, 'S2024004', '刘同学', '数学2024-1班', '数学与应用数学', '2024', '13900139004', 'liuxue@edu.cn', 'http://localhost:9000/education-files/avatars/student4-avatar.jpg'),
(11, 'S2024005', '陈同学', '数学2024-1班', '数学与应用数学', '2024', '13900139005', 'chenxue@edu.cn', 'http://localhost:9000/education-files/avatars/student5-avatar.jpg'),
(12, 'S2024006', '赵同学', '物理2024-1班', '物理学', '2024', '13900139006', 'zhaoxue@edu.cn', 'http://localhost:9000/education-files/avatars/student6-avatar.jpg'),
(13, 'S2024007', '孙同学', '英语2024-1班', '英语', '2024', '13900139007', 'sunxue@edu.cn', 'http://localhost:9000/education-files/avatars/student7-avatar.jpg'),
(14, 'S2024008', '周同学', '经济2024-1班', '经济学', '2024', '13900139008', 'zhouxue@edu.cn', 'http://localhost:9000/education-files/avatars/student8-avatar.jpg'),
(15, 'S2024009', '吴同学', '计科2024-2班', '计算机科学与技术', '2024', '13900139009', 'wuxue@edu.cn', 'http://localhost:9000/education-files/avatars/student9-avatar.jpg'),
(16, 'S2024010', '郑同学', '数学2024-2班', '数学与应用数学', '2024', '13900139010', 'zhengxue@edu.cn', 'http://localhost:9000/education-files/avatars/student10-avatar.jpg');

-- 插入课程信息（包含封面图片）
INSERT INTO `course` (`course_code`, `course_name`, `teacher_id`, `teacher_name`, `description`, `cover_image`, `credit`, `total_capacity`, `selected_count`, `status`, `start_time`, `end_time`) VALUES
('CS101', 'Java程序设计', 1, '张明', '本课程主要介绍Java语言的基础语法、面向对象编程、集合框架、IO流、多线程等内容，通过理论学习和实践操作，使学生掌握Java编程的基本技能。', 'http://localhost:9000/education-files/course-covers/java-cover.jpg', 4.0, 50, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59'),
('CS102', '数据结构与算法', 1, '张明', '本课程系统地介绍各种数据结构（线性表、栈、队列、树、图等）和常用算法（排序、查找、图算法等），培养学生分析和解决问题的能力。', 'http://localhost:9000/education-files/course-covers/datastructure-cover.jpg', 4.0, 45, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59'),
('CS103', '数据库系统原理', 1, '张明', '本课程介绍数据库系统的基本概念、关系数据模型、SQL语言、数据库设计、事务处理等内容，使学生掌握数据库系统的设计和管理能力。', 'http://localhost:9000/education-files/course-covers/database-cover.jpg', 3.5, 40, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59'),
('MATH201', '高等数学A', 2, '王芳', '本课程是理工科学生的重要基础课程，内容包括函数、极限、连续、导数、微分、积分、级数等，为后续专业课程打下坚实的数学基础。', 'http://localhost:9000/education-files/course-covers/calculus-cover.jpg', 5.0, 60, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59'),
('MATH202', '线性代数', 2, '王芳', '本课程介绍向量空间、矩阵、行列式、线性方程组、特征值与特征向量等内容，是计算机科学、物理学等专业的重要数学工具。', 'http://localhost:9000/education-files/course-covers/linear-algebra-cover.jpg', 3.0, 55, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59'),
('PHY301', '大学物理', 3, '李强', '本课程系统地介绍力学、热学、电磁学、光学等物理学基础知识，培养学生的物理思维和实验能力。', 'http://localhost:9000/education-files/course-covers/physics-cover.jpg', 4.0, 50, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59'),
('ENG401', '大学英语', 4, '刘静', '本课程旨在提高学生的英语综合应用能力，包括听、说、读、写、译等方面，为学生的国际化发展打下基础。', 'http://localhost:9000/education-files/course-covers/english-cover.jpg', 3.0, 80, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59'),
('ENG402', '英语口语', 4, '刘静', '本课程通过情景对话、角色扮演、演讲等方式，提高学生的英语口语表达能力和跨文化交际能力。', 'http://localhost:9000/education-files/course-covers/english-speaking-cover.jpg', 2.0, 30, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59'),
('ECO501', '微观经济学', 5, '陈伟', '本课程介绍市场机制、消费者行为、生产者行为、市场结构等微观经济学基本理论，帮助学生理解市场经济运行规律。', 'http://localhost:9000/education-files/course-covers/microeconomics-cover.jpg', 3.5, 45, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59'),
('ECO502', '宏观经济学', 5, '陈伟', '本课程介绍国民收入、经济增长、通货膨胀、失业、财政政策、货币政策等宏观经济学内容，帮助学生理解宏观经济运行。', 'http://localhost:9000/education-files/course-covers/macroeconomics-cover.jpg', 3.5, 45, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59'),
('CS201', 'Web前端开发', 1, '张明', '本课程介绍HTML、CSS、JavaScript等前端技术，以及Vue、React等现代前端框架，培养学生开发交互式网页的能力。', 'http://localhost:9000/education-files/course-covers/web-frontend-cover.jpg', 3.0, 35, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59'),
('CS202', 'Spring Boot框架', 1, '张明', '本课程介绍Spring Boot框架的使用，包括自动配置、依赖注入、AOP、数据访问、Web开发等内容，培养学生开发企业级应用的能力。', 'http://localhost:9000/education-files/course-covers/springboot-cover.jpg', 3.5, 40, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59'),
('CS203', '软件工程', 1, '张明', '本课程介绍软件开发的全过程，包括需求分析、系统设计、编码实现、测试、维护等，培养学生软件工程实践能力。', 'http://localhost:9000/education-files/course-covers/software-engineering-cover.jpg', 3.0, 50, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59'),
('MATH301', '概率论与数理统计', 2, '王芳', '本课程介绍随机事件、概率、随机变量、分布函数、数理统计等内容，为数据科学和机器学习提供数学基础。', 'http://localhost:9000/education-files/course-covers/probability-cover.jpg', 3.5, 50, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59'),
('CS301', '人工智能基础', 1, '张明', '本课程介绍人工智能的基本概念、机器学习、深度学习、自然语言处理等内容，培养学生对AI技术的理解和应用能力。', 'http://localhost:9000/education-files/course-covers/ai-cover.jpg', 3.0, 30, 0, 1, '2024-09-01 00:00:00', '2024-12-31 23:59:59');

-- 插入选课记录
INSERT INTO `course_selection` (`student_id`, `course_id`, `status`, `selection_time`, `approve_time`) VALUES
(1, 1, 1, '2024-09-05 10:00:00', '2024-09-05 14:00:00'),
(1, 2, 1, '2024-09-05 10:05:00', '2024-09-05 14:05:00'),
(1, 4, 1, '2024-09-05 10:10:00', '2024-09-05 14:10:00'),
(2, 1, 1, '2024-09-05 11:00:00', '2024-09-05 15:00:00'),
(2, 3, 0, '2024-09-05 11:05:00', NULL),
(2, 7, 1, '2024-09-05 11:10:00', '2024-09-05 15:10:00'),
(3, 1, 1, '2024-09-05 12:00:00', '2024-09-05 16:00:00'),
(3, 5, 1, '2024-09-05 12:05:00', '2024-09-05 16:05:00'),
(3, 11, 0, '2024-09-05 12:10:00', NULL),
(4, 4, 1, '2024-09-05 13:00:00', '2024-09-05 17:00:00'),
(4, 5, 1, '2024-09-05 13:05:00', '2024-09-05 17:05:00'),
(4, 14, 1, '2024-09-05 13:10:00', '2024-09-05 17:10:00'),
(5, 4, 1, '2024-09-05 14:00:00', '2024-09-05 18:00:00'),
(5, 5, 1, '2024-09-05 14:05:00', '2024-09-05 18:05:00'),
(6, 6, 1, '2024-09-05 15:00:00', '2024-09-05 19:00:00'),
(6, 4, 1, '2024-09-05 15:05:00', '2024-09-05 19:05:00'),
(7, 7, 1, '2024-09-05 16:00:00', '2024-09-05 20:00:00'),
(7, 8, 0, '2024-09-05 16:05:00', NULL),
(8, 9, 1, '2024-09-05 17:00:00', '2024-09-05 21:00:00'),
(8, 10, 1, '2024-09-05 17:05:00', '2024-09-05 21:05:00'),
(9, 1, 1, '2024-09-05 18:00:00', '2024-09-05 22:00:00'),
(9, 11, 1, '2024-09-05 18:05:00', '2024-09-05 22:05:00'),
(10, 5, 1, '2024-09-05 19:00:00', '2024-09-05 23:00:00'),
(10, 14, 0, '2024-09-05 19:05:00', NULL);

-- 更新课程的已选人数
UPDATE `course` SET `selected_count` = (
    SELECT COUNT(*) FROM `course_selection` 
    WHERE `course_selection`.`course_id` = `course`.`id` AND `status` = 1
);

-- 插入消息记录
INSERT INTO `message` (`sender_id`, `receiver_id`, `content`, `type`, `status`, `create_time`) VALUES
(1, 7, '欢迎选课！如有问题请随时联系。', 'TEXT', 1, '2024-09-05 10:00:00'),
(7, 1, '老师，我想了解一下Java课程的具体安排。', 'TEXT', 1, '2024-09-05 10:30:00'),
(1, 7, '课程安排已发送到你的邮箱，请查收。', 'TEXT', 0, '2024-09-05 11:00:00'),
(1, 8, '你的选课申请已通过，请按时上课。', 'TEXT', 1, '2024-09-05 15:00:00'),
(8, 1, '谢谢老师！', 'TEXT', 1, '2024-09-05 15:30:00'),
(2, 7, '高等数学课程本周开始，请做好准备。', 'TEXT', 1, '2024-09-06 09:00:00'),
(7, 2, '好的，老师。', 'TEXT', 1, '2024-09-06 09:15:00'),
(1, 9, '数据结构课程需要提前预习第一章内容。', 'TEXT', 0, '2024-09-06 10:00:00'),
(4, 7, '英语口语课程需要准备自我介绍。', 'TEXT', 1, '2024-09-06 11:00:00'),
(5, 8, '经济学课程推荐阅读《经济学原理》。', 'TEXT', 1, '2024-09-06 14:00:00');

