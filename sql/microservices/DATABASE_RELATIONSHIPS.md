# 微服务数据库关联关系文档

## 概述

本文档详细说明各个微服务数据库之间的关联关系。由于采用微服务架构，每个服务拥有独立数据库，**跨数据库的外键约束已移除**，需要通过**应用层**和**服务间调用**来维护数据一致性。

## 数据库关联关系图

```
┌─────────────────────────────────────────────────────────────────┐
│                    user_service_db                              │
│  ┌──────────────┐         ┌──────────────────┐                 │
│  │  students    │◄────────│ user_credentials │                 │
│  │  (student_id)│         │  (student_id FK) │                 │
│  └──────────────┘         └──────────────────┘                 │
└─────────────────────────────────────────────────────────────────┘
         │
         │ student_id (逻辑关联)
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│                enrollment_service_db                            │
│  ┌──────────────────────────────────────────────┐              │
│  │         enrollments                          │              │
│  │  student_id (来自user-service)               │              │
│  │  course_id (来自course-service)              │              │
│  └──────────────────────────────────────────────┘              │
└─────────────────────────────────────────────────────────────────┘
         │
         │ course_id (逻辑关联)
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    course_service_db                            │
│  ┌──────────────┐         ┌──────────────────┐                 │
│  │   courses    │◄────────│ course_schedule  │                 │
│  │  (course_id) │         │  (course_id FK)  │                 │
│  │ teacher_id   │         └──────────────────┘                 │
│  │ (来自teacher)│                                               │
│  └──────────────┘                                               │
└─────────────────────────────────────────────────────────────────┘
         │
         │ teacher_id (逻辑关联)
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│                  teacher_service_db                             │
│  ┌──────────────┐         ┌──────────────────┐                 │
│  │   teachers   │◄────────│ teacher_courses  │                 │
│  │ (teacher_id) │         │  (teacher_id FK) │                 │
│  └──────────────┘         │  course_id       │                 │
│                            │ (来自course)     │                 │
│                            └──────────────────┘                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    file_service_db                              │
│  ┌──────────────────────────────────────────────┐              │
│  │              files                            │              │
│  │  uploaded_by (student_id 或 teacher_id)      │              │
│  │  uploaded_by_type (STUDENT/TEACHER/ADMIN)    │              │
│  └──────────────────────────────────────────────┘              │
└─────────────────────────────────────────────────────────────────┘
         │
         │ uploaded_by (逻辑关联)
         │
         ├─────────────────┬─────────────────┐
         │                 │                 │
         ▼                 ▼                 ▼
    user_service    teacher_service    (ADMIN)
    (student_id)    (teacher_id)

┌─────────────────────────────────────────────────────────────────┐
│                  message_service_db                             │
│  ┌──────────────────────────────────────────────┐              │
│  │            messages                           │              │
│  │  sender_id (student_id 或 teacher_id)        │              │
│  │  receiver_id (student_id 或 teacher_id)      │              │
│  │  sender_type / receiver_type                  │              │
│  └──────────────────────────────────────────────┘              │
│  ┌──────────────────────────────────────────────┐              │
│  │            sessions                           │              │
│  │  user_id (student_id 或 teacher_id)          │              │
│  │  user_type (STUDENT/TEACHER/ADMIN)           │              │
│  └──────────────────────────────────────────────┘              │
└─────────────────────────────────────────────────────────────────┘
         │
         │ user_id (逻辑关联)
         │
         ├─────────────────┬─────────────────┐
         │                 │                 │
         ▼                 ▼                 ▼
    user_service    teacher_service    (ADMIN)
    (student_id)    (teacher_id)
```

## 详细关联关系说明

### 1. user_service_db（学生服务）

#### 内部关联
- **students ↔ user_credentials**
  - 关系：**1:1**（一个学生对应一个登录凭证）
  - 外键：`user_credentials.student_id` → `students.student_id`
  - 约束：数据库级外键约束（ON DELETE CASCADE）
  - 说明：学生信息和登录凭证在同一数据库内，使用数据库外键约束

### 2. teacher_service_db（教师服务）

#### 内部关联
- **teachers ↔ teacher_courses**
  - 关系：**1:N**（一个教师可以教授多门课程）
  - 外键：`teacher_courses.teacher_id` → `teachers.teacher_id`
  - 约束：数据库级外键约束（ON DELETE CASCADE）

#### 跨服务关联
- **teacher_courses.course_id → course_service_db.courses.course_id**
  - 关系：**逻辑关联**（无数据库外键约束）
  - 说明：`course_id` 来自 `course-service`，需要通过服务间调用验证

### 3. course_service_db（课程服务）

#### 内部关联
- **courses ↔ course_schedule**
  - 关系：**1:N**（一门课程可以有多个上课时间）
  - 外键：`course_schedule.course_id` → `courses.course_id`
  - 约束：数据库级外键约束（ON DELETE CASCADE）

#### 跨服务关联
- **courses.teacher_id → teacher_service_db.teachers.teacher_id**
  - 关系：**逻辑关联**（无数据库外键约束）
  - 说明：`teacher_id` 来自 `teacher-service`，需要通过服务间调用验证
  - 冗余字段：`courses.teacher_name` 用于提高查询性能，避免频繁跨服务调用

### 4. enrollment_service_db（选课服务）

#### 跨服务关联
- **enrollments.student_id → user_service_db.students.student_id**
  - 关系：**逻辑关联**（无数据库外键约束）
  - 说明：`student_id` 来自 `user-service`，需要通过服务间调用验证学生是否存在

- **enrollments.course_id → course_service_db.courses.course_id**
  - 关系：**逻辑关联**（无数据库外键约束）
  - 说明：`course_id` 来自 `course-service`，需要通过服务间调用验证课程是否存在

- **唯一约束**：`(student_id, course_id)` 确保一个学生不能重复选同一门课程

### 5. file_service_db（文件服务）

#### 跨服务关联
- **files.uploaded_by → user_service_db.students.student_id**（当 `uploaded_by_type = 'STUDENT'`）
  - 关系：**逻辑关联**（无数据库外键约束）
  - 说明：文件上传者可能是学生，需要通过服务间调用验证

- **files.uploaded_by → teacher_service_db.teachers.teacher_id**（当 `uploaded_by_type = 'TEACHER'`）
  - 关系：**逻辑关联**（无数据库外键约束）
  - 说明：文件上传者可能是教师，需要通过服务间调用验证

- **files.uploaded_by = 0**（当 `uploaded_by_type = 'ADMIN'`）
  - 说明：管理员上传的文件，`uploaded_by = 0` 表示管理员

### 6. message_service_db（消息服务）

#### 跨服务关联
- **messages.sender_id → user_service_db.students.student_id**（当 `sender_type = 'STUDENT'`）
  - 关系：**逻辑关联**（无数据库外键约束）
  - 说明：消息发送者可能是学生

- **messages.sender_id → teacher_service_db.teachers.teacher_id**（当 `sender_type = 'TEACHER'`）
  - 关系：**逻辑关联**（无数据库外键约束）
  - 说明：消息发送者可能是教师

- **messages.receiver_id → user_service_db.students.student_id**（当 `receiver_type = 'STUDENT'`）
  - 关系：**逻辑关联**（无数据库外键约束）
  - 说明：消息接收者可能是学生

- **messages.receiver_id → teacher_service_db.teachers.teacher_id**（当 `receiver_type = 'TEACHER'`）
  - 关系：**逻辑关联**（无数据库外键约束）
  - 说明：消息接收者可能是教师

- **sessions.user_id → user_service_db.students.student_id**（当 `user_type = 'STUDENT'`）
  - 关系：**逻辑关联**（无数据库外键约束）
  - 说明：WebSocket会话的用户可能是学生

- **sessions.user_id → teacher_service_db.teachers.teacher_id**（当 `user_type = 'TEACHER'`）
  - 关系：**逻辑关联**（无数据库外键约束）
  - 说明：WebSocket会话的用户可能是教师

## 服务间调用关系

### 1. enrollment-service 调用其他服务

```java
// 选课时需要验证学生和课程是否存在
@FeignClient(name = "user-service")
public interface UserServiceClient {
    @GetMapping("/student/{studentId}")
    Result<StudentDTO> getStudentById(@PathVariable Long studentId);
}

@FeignClient(name = "course-service")
public interface CourseServiceClient {
    @GetMapping("/course/{courseId}")
    Result<CourseDTO> getCourseById(@PathVariable Long courseId);
    
    @PutMapping("/course/{courseId}/increment-selected")
    Result<?> incrementSelectedCount(@PathVariable Long courseId);
}
```

### 2. course-service 调用 teacher-service

```java
// 创建课程时需要验证教师是否存在
@FeignClient(name = "teacher-service")
public interface TeacherServiceClient {
    @GetMapping("/teacher/{teacherId}")
    Result<TeacherDTO> getTeacherById(@PathVariable Long teacherId);
}
```

### 3. file-service 调用其他服务

```java
// 上传文件时需要验证上传者身份
@FeignClient(name = "user-service")
public interface UserServiceClient {
    @GetMapping("/student/{studentId}")
    Result<StudentDTO> getStudentById(@PathVariable Long studentId);
}

@FeignClient(name = "teacher-service")
public interface TeacherServiceClient {
    @GetMapping("/teacher/{teacherId}")
    Result<TeacherDTO> getTeacherById(@PathVariable Long teacherId);
}
```

### 4. message-service 调用其他服务

```java
// 发送消息时需要验证发送者和接收者身份
@FeignClient(name = "user-service")
public interface UserServiceClient {
    @GetMapping("/student/{studentId}")
    Result<StudentDTO> getStudentById(@PathVariable Long studentId);
}

@FeignClient(name = "teacher-service")
public interface TeacherServiceClient {
    @GetMapping("/teacher/{teacherId}")
    Result<TeacherDTO> getTeacherById(@PathVariable Long teacherId);
}
```

## 数据一致性保证

### 1. 最终一致性（Eventual Consistency）

由于跨服务无法使用分布式事务，采用最终一致性模式：

- **选课流程**：
  1. enrollment-service 创建选课记录
  2. 发送消息到 RabbitMQ，通知 course-service 更新已选人数
  3. course-service 异步处理消息，更新 `selected_count`

- **删除学生**：
  1. user-service 删除学生记录
  2. 发送消息到 RabbitMQ，通知相关服务
  3. enrollment-service 异步处理，删除相关选课记录
  4. message-service 异步处理，删除相关消息记录

### 2. Saga 模式

对于复杂的业务流程，使用 Saga 模式：

```java
// 选课 Saga
1. enrollment-service: 创建选课记录
2. course-service: 增加已选人数（如果失败，回滚步骤1）
3. teacher-service: 更新教师课程关联（可选）
```

### 3. 冗余字段

为了提高查询性能，使用冗余字段：

- `courses.teacher_name`：避免频繁查询 teacher-service
- `messages.sender_type` / `receiver_type`：标识用户类型，避免跨服务查询

## 注意事项

### 1. ID 映射

不同服务使用不同的ID体系：
- `user_service_db`: `student_id`
- `teacher_service_db`: `teacher_id`
- `course_service_db`: `course_id`
- `enrollment_service_db`: `enrollment_id`

**需要维护ID映射关系**，或者使用全局唯一ID（如UUID）。

### 2. 外键约束

**跨服务的外键约束已移除**，需要通过应用层保证：
- 创建记录前验证关联数据是否存在
- 删除记录时处理级联删除（通过消息队列）

### 3. 查询优化

避免跨服务查询：
- 使用冗余字段（如 `teacher_name`）
- 使用缓存（Redis）存储常用数据
- 批量查询，减少服务间调用次数

### 4. 事务管理

- **服务内部**：使用本地事务（@Transactional）
- **跨服务**：使用 Saga 模式或最终一致性

## 关联关系总结表

| 源服务 | 源表/字段 | 目标服务 | 目标表/字段 | 关系类型 | 约束方式 |
|--------|----------|----------|------------|----------|----------|
| user-service | students.student_id | enrollment-service | enrollments.student_id | 逻辑关联 | 应用层验证 |
| course-service | courses.course_id | enrollment-service | enrollments.course_id | 逻辑关联 | 应用层验证 |
| teacher-service | teachers.teacher_id | course-service | courses.teacher_id | 逻辑关联 | 应用层验证 |
| course-service | courses.course_id | teacher-service | teacher_courses.course_id | 逻辑关联 | 应用层验证 |
| user-service | students.student_id | file-service | files.uploaded_by | 逻辑关联 | 应用层验证 |
| teacher-service | teachers.teacher_id | file-service | files.uploaded_by | 逻辑关联 | 应用层验证 |
| user-service | students.student_id | message-service | messages.sender_id/receiver_id | 逻辑关联 | 应用层验证 |
| teacher-service | teachers.teacher_id | message-service | messages.sender_id/receiver_id | 逻辑关联 | 应用层验证 |
| user-service | students.student_id | message-service | sessions.user_id | 逻辑关联 | 应用层验证 |
| teacher-service | teachers.teacher_id | message-service | sessions.user_id | 逻辑关联 | 应用层验证 |

## 实现建议

1. **使用 Feign Client** 进行服务间调用
2. **使用 RabbitMQ** 进行异步消息通信
3. **使用 Redis** 缓存常用数据，减少跨服务调用
4. **实现 Saga 模式** 处理复杂业务流程
5. **添加数据验证** 在创建/更新记录前验证关联数据
6. **实现补偿机制** 处理失败场景的回滚

