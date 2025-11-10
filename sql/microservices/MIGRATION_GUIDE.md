# 数据库迁移指南

## 概述

本指南说明如何从单数据库架构迁移到微服务独立数据库架构。

## 迁移步骤

### 1. 备份现有数据

```bash
# 备份原数据库
mysqldump -u root -p education_management > backup_education_management.sql
```

### 2. 创建新数据库

执行所有微服务的数据库初始化脚本：

```bash
mysql -u root -p < sql/microservices/user-service-db.sql
mysql -u root -p < sql/microservices/teacher-service-db.sql
mysql -u root -p < sql/microservices/course-service-db.sql
mysql -u root -p < sql/microservices/enrollment-service-db.sql
mysql -u root -p < sql/microservices/file-service-db.sql
mysql -u root -p < sql/microservices/message-service-db.sql
```

### 3. 数据迁移脚本

由于表结构变化较大，需要编写数据迁移脚本。以下是迁移逻辑：

#### 3.1 学生数据迁移（user-service）

```sql
-- 从旧表迁移到新表
INSERT INTO user_service_db.students (name, gender, major, grade, class_name, contact_info, avatar_url, status, created_at, updated_at)
SELECT 
    real_name as name,
    NULL as gender,  -- 需要手动补充
    major,
    grade,
    class_name,
    CONCAT('{"phone":"', IFNULL(phone, ''), '","email":"', IFNULL(email, ''), '"}') as contact_info,
    avatar as avatar_url,
    status,
    create_time as created_at,
    update_time as updated_at
FROM education_management.student;

-- 迁移登录凭证
INSERT INTO user_service_db.user_credentials (student_id, username, password_hash, email, phone, role, status, created_at, updated_at)
SELECT 
    s.student_id,
    u.username,
    u.password as password_hash,
    u.email,
    u.phone,
    u.role,
    u.status,
    u.create_time as created_at,
    u.update_time as updated_at
FROM education_management.sys_user u
JOIN user_service_db.students s ON u.real_name = s.name
WHERE u.role = 'STUDENT';
```

#### 3.2 教师数据迁移（teacher-service）

```sql
INSERT INTO teacher_service_db.teachers (name, gender, department, title, contact_info, avatar_url, status, created_at, updated_at)
SELECT 
    real_name as name,
    NULL as gender,  -- 需要手动补充
    department,
    title,
    CONCAT('{"phone":"', IFNULL(phone, ''), '","email":"', IFNULL(email, ''), '"}') as contact_info,
    avatar as avatar_url,
    1 as status,
    create_time as created_at,
    update_time as updated_at
FROM education_management.teacher;
```

#### 3.3 课程数据迁移（course-service）

```sql
INSERT INTO course_service_db.courses (course_code, course_name, teacher_id, teacher_name, course_description, credit, total_capacity, selected_count, cover_image, status, start_time, end_time, department, created_at, updated_at)
SELECT 
    course_code,
    course_name,
    teacher_id,
    teacher_name,
    description as course_description,
    credit,
    total_capacity,
    selected_count,
    cover_image,
    status,
    start_time,
    end_time,
    NULL as department,  -- 需要手动补充
    create_time as created_at,
    update_time as updated_at
FROM education_management.course;
```

#### 3.4 选课数据迁移（enrollment-service）

```sql
INSERT INTO enrollment_service_db.enrollments (student_id, course_id, enrollment_time, status, created_at, updated_at)
SELECT 
    student_id,
    course_id,
    selection_time as enrollment_time,
    CASE WHEN status = 1 THEN 0 ELSE 1 END as status,  -- 0-已选，1-已退
    create_time as created_at,
    update_time as updated_at
FROM education_management.course_selection;
```

#### 3.5 消息数据迁移（message-service）

```sql
INSERT INTO message_service_db.messages (sender_id, sender_type, receiver_id, receiver_type, message_type, content, status, created_at, updated_at)
SELECT 
    sender_id,
    (SELECT role FROM education_management.sys_user WHERE id = sender_id) as sender_type,
    receiver_id,
    (SELECT role FROM education_management.sys_user WHERE id = receiver_id) as receiver_type,
    type as message_type,
    content,
    status,
    create_time as created_at,
    create_time as updated_at
FROM education_management.message;
```

### 4. 验证数据

```sql
-- 检查各服务的数据量
SELECT 'students' as table_name, COUNT(*) as count FROM user_service_db.students
UNION ALL
SELECT 'teachers', COUNT(*) FROM teacher_service_db.teachers
UNION ALL
SELECT 'courses', COUNT(*) FROM course_service_db.courses
UNION ALL
SELECT 'enrollments', COUNT(*) FROM enrollment_service_db.enrollments
UNION ALL
SELECT 'messages', COUNT(*) FROM message_service_db.messages;
```

### 5. 更新应用配置

确保所有服务的 `application.yml` 已更新为新的数据库连接。

### 6. 测试验证

1. 启动所有微服务
2. 测试各个API接口
3. 验证数据一致性
4. 检查服务间通信

## 注意事项

1. **ID映射**：新数据库使用新的ID体系，需要维护ID映射关系
2. **外键约束**：跨服务的外键已移除，需要通过应用层保证一致性
3. **数据完整性**：迁移后需要验证数据完整性
4. **回滚方案**：保留原数据库备份，以便回滚

## 回滚方案

如果迁移失败，可以：

1. 停止所有微服务
2. 恢复原数据库配置
3. 恢复原数据库备份：
   ```bash
   mysql -u root -p education_management < backup_education_management.sql
   ```

