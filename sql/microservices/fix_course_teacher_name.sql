-- ============================================
-- 修复课程表中缺失的教师姓名
-- 执行此脚本可以修复已创建但teacher_name为空的课程记录
-- ============================================

USE `course_service_db`;

-- 查询需要修复的课程（teacher_id不为空但teacher_name为空）
SELECT 
    c.course_id,
    c.course_name,
    c.teacher_id,
    c.teacher_name,
    t.name AS teacher_name_from_teacher_service
FROM courses c
LEFT JOIN teacher_service_db.teachers t ON c.teacher_id = t.teacher_id
WHERE c.teacher_id IS NOT NULL 
  AND (c.teacher_name IS NULL OR c.teacher_name = '');

-- 更新课程表中的teacher_name（从teacher_service_db.teachers表获取）
UPDATE courses c
INNER JOIN teacher_service_db.teachers t ON c.teacher_id = t.teacher_id
SET c.teacher_name = t.name,
    c.department = COALESCE(c.department, t.department),
    c.updated_at = NOW()
WHERE c.teacher_id IS NOT NULL 
  AND (c.teacher_name IS NULL OR c.teacher_name = '')
  AND t.name IS NOT NULL;

-- 验证修复结果
SELECT 
    course_id,
    course_name,
    teacher_id,
    teacher_name,
    department
FROM courses
WHERE teacher_id IS NOT NULL
ORDER BY course_id;

