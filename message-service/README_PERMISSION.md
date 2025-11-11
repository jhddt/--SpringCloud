# 消息服务权限系统重构说明

## 📋 概述

按照学习通消息权限划分机制，对消息服务进行了全面重构，实现了完整的权限控制体系。

## 🎯 核心功能

### 1. 消息类型

- **INSTANT_MESSAGE（即时消息）**: 师生交流、群聊
- **SYSTEM_NOTICE（系统通知）**: 作业、考试通知
- **INTERACTION_REMINDER（互动提醒）**: 评论、点赞、签到
- **PLATFORM_ANNOUNCEMENT（平台公告）**: 课程通知、系统公告

### 2. 范围类型

- **PRIVATE（私聊）**: 点对点消息
- **COURSE（课程）**: 课程范围内的消息
- **GROUP（群组）**: 群组消息
- **GLOBAL（全局）**: 平台公告

### 3. 权限控制

#### 发送权限
- **即时消息**: 学生、教师、管理员
- **系统通知**: 教师、管理员（只能发送到自己课程的课程成员）
- **互动提醒**: 系统自动生成
- **平台公告**: 管理员

#### 接收权限
- **私聊消息**: 只有指定的接收者可以接收
- **课程消息**: 只有课程成员可以接收（学生、教师）
- **群组消息**: 只有群组成员可以接收
- **全局消息**: 根据角色掩码确定接收者

### 4. 数据隔离

- 学生只能看到自己发送的、接收的、或所在课程/群组的消息
- 教师只能看到自己发送的、接收的、或自己课程的课程消息
- 管理员可以看到所有消息

## 🔧 技术实现

### 1. 数据库变更

执行 `sql/microservices/message-service-permission-upgrade.sql` 脚本，添加以下字段：
- `content_type`: 消息内容类型
- `scope_type`: 消息范围类型
- `scope_id`: 所属对象ID
- `role_mask`: 可见角色标识

### 2. 权限验证服务

`MessagePermissionService` 负责：
- 发送权限验证
- 接收权限验证
- 课程成员关系验证
- 角色权限验证

### 3. Feign客户端

- `CourseServiceClient`: 获取课程信息
- `StudentServiceClient`: 获取学生信息
- `TeacherServiceClient`: 获取教师信息
- `SelectionServiceClient`: 验证选课关系

### 4. 消息服务

`MessageService` 重构后：
- 发送消息时进行权限验证
- 查询消息时进行数据隔离
- 自动填充用户名称
- WebSocket通知支持

## 📝 API变更

### 发送消息

**旧接口（已废弃）**:
```http
POST /message/send
Content-Type: application/json

{
  "senderId": 1,
  "senderType": "TEACHER",
  "receiverId": 2,
  "receiverType": "STUDENT",
  "content": "消息内容"
}
```

**新接口**:
```http
POST /message/send
X-User-Id: 1
X-Role: TEACHER
Content-Type: application/json

{
  "receiverId": 2,
  "receiverType": "STUDENT",
  "messageType": "INSTANT_MESSAGE",
  "scopeType": "PRIVATE",
  "contentType": "TEXT",
  "content": "消息内容"
}
```

### 查询消息

**新接口**:
```http
GET /message/page?current=1&size=20&messageType=SYSTEM_NOTICE&scopeType=COURSE&scopeId=1
X-User-Id: 1
X-Role: STUDENT
```

## 🚀 部署步骤

1. **执行数据库迁移脚本**
   ```sql
   source sql/microservices/message-service-permission-upgrade.sql
   ```

2. **更新依赖**
   - 确保 `message-service/pom.xml` 中包含 `spring-cloud-starter-openfeign` 依赖

3. **重启服务**
   - 重启 `message-service`
   - 确保 `course-service`、`student-service`、`teacher-service`、`selection-service` 正常运行

4. **验证功能**
   - 测试发送不同类型的消息
   - 验证权限控制是否生效
   - 验证数据隔离是否正确

## 📊 权限矩阵

| 消息类型 | 发送者角色 | 接收者 | 范围限制 |
|---------|-----------|--------|---------|
| 即时消息 | 学生/教师/管理员 | 同群成员 | 课程/群组/私聊 |
| 系统通知 | 教师/管理员 | 课程成员 | 课程 |
| 互动提醒 | 系统 | 被提醒用户 | 根据业务 |
| 平台公告 | 管理员 | 所有用户 | 全局 |

## 🔒 安全特性

1. **身份认证**: 所有消息操作都需要用户身份验证
2. **授权检查**: 发送和接收消息前都会进行权限验证
3. **数据隔离**: 用户只能看到自己有权限的消息
4. **角色掩码**: 支持细粒度的角色级别权限控制

## 📚 相关文档

- [学习通消息权限划分机制](./docs/MESSAGE_PERMISSION.md)
- [数据库设计](./sql/microservices/message-service-db.sql)
- [API文档](./docs/API.md)

