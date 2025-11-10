-- 检查数据库中的数据统计
USE `education_management`;

-- 检查学生数量
SELECT COUNT(*) as student_count FROM `student`;

-- 检查教师数量
SELECT COUNT(*) as teacher_count FROM `teacher`;

-- 检查课程数量
SELECT COUNT(*) as course_count FROM `course`;

-- 检查选课数量
SELECT COUNT(*) as selection_count FROM `course_selection`;

-- 检查用户数量
SELECT COUNT(*) as user_count FROM `sys_user`;

-- 查看前5条学生记录
SELECT * FROM `student` LIMIT 5;

-- 查看前5条教师记录
SELECT * FROM `teacher` LIMIT 5;

-- 查看前5条课程记录
SELECT * FROM `course` LIMIT 5;

