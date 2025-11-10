# 微服务数据库重构说明

## 概述

本项目已从单数据库架构重构为微服务架构，每个服务拥有独立的数据库。

## 数据库列表

| 服务名称 | 数据库名 | 主要表 | 说明 |
|---------|---------|--------|------|
| user-service (student-service) | `user_service_db` | `students`, `user_credentials` | 学生信息管理和登录凭证 |
| teacher-service | `teacher_service_db` | `teachers`, `teacher_courses` | 教师信息管理和课程关联 |
| course-service | `course_service_db` | `courses`, `course_schedule` | 课程信息管理和时间安排 |
| enrollment-service (selection-service) | `enrollment_service_db` | `enrollments` | 选课记录管理 |
| file-service | `file_service_db` | `files` | 文件元数据管理（MinIO存储） |
| message-service | `message_service_db` | `messages`, `sessions` | 消息通信和WebSocket会话 |

## 数据库初始化

执行以下SQL脚本创建所有数据库：

```bash
# 按顺序执行
mysql -u root -p < sql/microservices/user-service-db.sql
mysql -u root -p < sql/microservices/teacher-service-db.sql
mysql -u root -p < sql/microservices/course-service-db.sql
mysql -u root -p < sql/microservices/enrollment-service-db.sql
mysql -u root -p < sql/microservices/file-service-db.sql
mysql -u root -p < sql/microservices/message-service-db.sql
```

## 配置更新

所有服务的 `application.yml` 已更新，指向各自的数据库：

- `student-service`: `user_service_db`
- `teacher-service`: `teacher_service_db`
- `course-service`: `course_service_db`
- `selection-service`: `enrollment_service_db`
- `file-service`: `file_service_db`
- `message-service`: `message_service_db`

## 重构进度

### ✅ 已完成
1. ✅ 创建所有微服务的数据库SQL脚本
2. ✅ 更新所有服务的数据库配置
3. ✅ 重构 student-service 的实体类（Student, UserCredential）
4. ✅ 重构 student-service 的 Mapper（StudentMapper, UserCredentialMapper）

### 🔄 进行中
- 重构 student-service 的 Service 和 Controller

### ⏳ 待完成
- 重构 teacher-service
- 重构 course-service
- 重构 enrollment-service (selection-service)
- 重构 file-service
- 重构 message-service
- 调整 auth-service 以适应新架构

## 重要变更说明

### 1. 表结构变更

#### students 表（原 student 表）
- `id` → `student_id`
- `real_name` → `name`
- 新增 `gender`, `date_of_birth`
- `phone`, `email` → `contact_info` (JSON格式)
- `avatar` → `avatar_url`
- `create_time` → `created_at`
- `update_time` → `updated_at`

#### user_credentials 表（原 sys_user 表的部分字段）
- 存储登录凭证信息
- `student_id` 外键关联 `students` 表
- `password` → `password_hash`

### 2. 服务间通信

由于每个服务拥有独立数据库，服务间需要通过以下方式通信：

1. **RESTful API调用**：使用 Feign 或 RestTemplate
2. **消息队列**：使用 RabbitMQ 进行异步通信
3. **事件驱动**：使用事件总线同步数据变更

### 3. 数据一致性

采用最终一致性（Eventual Consistency）模式：
- 避免跨服务事务
- 使用 Saga 模式处理复杂业务流程
- 通过消息队列保证数据最终一致

## 下一步工作

1. 完成 student-service 的 Service 和 Controller 重构
2. 重构其他服务的实体类、Mapper、Service
3. 实现服务间通信（Feign Client）
4. 更新前端API调用（如有需要）
5. 数据迁移脚本（从旧数据库迁移到新数据库）

## 注意事项

1. **外键约束**：跨服务的外键约束已移除，需要通过应用层保证数据一致性
2. **ID映射**：不同服务使用不同的ID体系，需要维护ID映射关系
3. **查询优化**：避免跨服务查询，使用冗余字段或缓存
4. **事务管理**：每个服务内部使用本地事务，跨服务使用Saga模式

