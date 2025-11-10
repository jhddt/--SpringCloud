# 数据库关联关系快速参考

## 核心关联关系

### 1. 学生选课流程
```
user_service_db.students (student_id)
    ↓ (逻辑关联)
enrollment_service_db.enrollments (student_id, course_id)
    ↓ (逻辑关联)
course_service_db.courses (course_id)
    ↓ (逻辑关联)
teacher_service_db.teachers (teacher_id)
```

### 2. 教师课程关联
```
teacher_service_db.teachers (teacher_id)
    ↓ (数据库外键)
teacher_service_db.teacher_courses (teacher_id, course_id)
    ↓ (逻辑关联)
course_service_db.courses (course_id)
```

### 3. 文件上传关联
```
file_service_db.files (uploaded_by)
    ├─→ user_service_db.students (student_id) [当 uploaded_by_type = 'STUDENT']
    ├─→ teacher_service_db.teachers (teacher_id) [当 uploaded_by_type = 'TEACHER']
    └─→ ADMIN [当 uploaded_by = 0]
```

### 4. 消息通信关联
```
message_service_db.messages
    ├─→ sender_id → user_service_db.students (student_id) [当 sender_type = 'STUDENT']
    ├─→ sender_id → teacher_service_db.teachers (teacher_id) [当 sender_type = 'TEACHER']
    ├─→ receiver_id → user_service_db.students (student_id) [当 receiver_type = 'STUDENT']
    └─→ receiver_id → teacher_service_db.teachers (teacher_id) [当 receiver_type = 'TEACHER']
```

## 关联类型说明

### 数据库级外键约束（同一数据库内）
- ✅ `user_credentials.student_id` → `students.student_id`
- ✅ `teacher_courses.teacher_id` → `teachers.teacher_id`
- ✅ `course_schedule.course_id` → `courses.course_id`

### 逻辑关联（跨数据库，无外键约束）
- ⚠️ `enrollments.student_id` → `students.student_id` (跨服务)
- ⚠️ `enrollments.course_id` → `courses.course_id` (跨服务)
- ⚠️ `courses.teacher_id` → `teachers.teacher_id` (跨服务)
- ⚠️ `files.uploaded_by` → `students.student_id` 或 `teachers.teacher_id` (跨服务)
- ⚠️ `messages.sender_id/receiver_id` → `students.student_id` 或 `teachers.teacher_id` (跨服务)

## 服务间调用依赖

```
enrollment-service
    ├─→ user-service (验证学生)
    └─→ course-service (验证课程、更新已选人数)

course-service
    └─→ teacher-service (验证教师)

file-service
    ├─→ user-service (验证学生上传者)
    └─→ teacher-service (验证教师上传者)

message-service
    ├─→ user-service (验证学生发送者/接收者)
    └─→ teacher-service (验证教师发送者/接收者)
```

## 数据一致性策略

| 场景 | 策略 | 说明 |
|------|------|------|
| 选课 | 最终一致性 + 消息队列 | 创建选课记录后，异步更新课程已选人数 |
| 删除学生 | 最终一致性 + 消息队列 | 删除学生后，异步删除相关选课和消息 |
| 创建课程 | Saga模式 | 验证教师 → 创建课程 → 更新教师课程关联 |
| 文件上传 | 同步验证 | 上传前同步验证上传者身份 |

## 关键字段说明

### 冗余字段（提高性能）
- `courses.teacher_name` - 避免频繁查询 teacher-service
- `messages.sender_type` / `receiver_type` - 标识用户类型
- `files.uploaded_by_type` - 标识上传者类型

### 类型标识字段
- `user_credentials.role` - ADMIN, TEACHER, STUDENT
- `messages.sender_type` / `receiver_type` - STUDENT, TEACHER, ADMIN
- `files.uploaded_by_type` - STUDENT, TEACHER, ADMIN
- `sessions.user_type` - STUDENT, TEACHER, ADMIN

