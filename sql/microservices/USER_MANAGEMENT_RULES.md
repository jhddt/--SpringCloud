# 用户管理规则文档

## 概述

本文档说明系统中用户创建、管理和密码管理的规则。

## 用户类型

系统支持三种用户类型：

1. **管理员（ADMIN）**
   - 拥有最高权限
   - 可以创建、修改、删除所有用户（包括教师和学生）
   - 可以管理所有系统功能

2. **教师（TEACHER）**
   - 可以管理自己教授的课程
   - 可以查看和审核选课申请
   - 可以与学生进行消息通信
   - **不能创建新用户**

3. **学生（STUDENT）**
   - 可以浏览和选择课程
   - 可以查看自己的选课记录和成绩
   - 可以与教师进行消息通信
   - **不能创建新用户**

## 用户创建规则

### 1. 只有管理员可以创建用户

- ✅ **管理员**：可以创建教师和学生账号
- ❌ **教师**：不能创建任何用户
- ❌ **学生**：不能创建任何用户
- ❌ **未登录用户**：不能创建任何用户

### 2. 创建用户时的要求

#### 创建学生账号
- 必须提供：姓名、学号（作为用户名）、专业、年级等基本信息
- 初始密码：**123456**（由系统自动设定，管理员无需指定）
- 登录账号：学号（如：S2024001）

#### 创建教师账号
- 必须提供：姓名、工号（作为用户名）、部门、职称等基本信息
- 初始密码：**123456**（由系统自动设定，管理员无需指定）
- 登录账号：工号（如：T001）

#### 创建管理员账号
- 管理员账号通常由系统初始化创建
- 初始密码：**123456**
- 登录账号：admin（或其他指定账号）

### 3. 权限验证

所有创建用户的接口都需要验证请求头中的 `X-Role`：

```java
@PostMapping
public Result<StudentDTO> create(
        @Valid @RequestBody StudentDTO dto,
        @RequestHeader(value = "X-Role", required = false) String role) {
    // 权限验证：只有管理员可以创建学生
    if (!StringUtils.hasText(role) || !Constants.ROLE_ADMIN.equals(role)) {
        throw new BusinessException(403, "只有管理员可以创建学生账号");
    }
    // ... 创建逻辑
}
```

## 登录认证规则

### 1. 登录方式

所有用户使用统一的登录接口：`POST /auth/login`

登录参数：
- `username`：用户名（学号/工号/管理员账号）
- `password`：密码
- `type`：登录类型（ADMIN/TEACHER/STUDENT）

### 2. 登录凭证存储

所有用户的登录凭证统一存储在 `user_service_db.user_credentials` 表中：

| 字段 | 说明 |
|------|------|
| `username` | 用户名（学号/工号/管理员账号），唯一 |
| `password_hash` | 密码哈希值（BCrypt加密） |
| `role` | 角色：ADMIN, TEACHER, STUDENT |
| `student_id` | 学生ID（当role=STUDENT时） |
| `teacher_id` | 教师ID（当role=TEACHER时） |
| `status` | 状态：0-禁用，1-启用 |

### 3. 初始密码

- **所有新创建的用户**（包括教师和学生）的初始密码都是 **123456**
- 初始密码由系统自动设定，管理员创建用户时无需指定密码
- 用户首次登录后应尽快修改密码

## 密码管理规则

### 1. 修改密码

- ✅ **所有用户**（管理员、教师、学生）都可以修改自己的密码
- ❌ **用户不能修改其他用户的密码**
- ❌ **管理员不能直接修改其他用户的密码**（需要通过重置密码功能）

### 2. 修改密码接口

**接口**：`PUT /auth/change-password`

**请求头**：
- `X-User-Id`：当前用户ID（由网关从JWT Token中提取）

**请求体**：
```json
{
  "oldPassword": "原密码",
  "newPassword": "新密码",
  "confirmPassword": "确认新密码"
}
```

**验证规则**：
1. 原密码必须正确
2. 新密码和确认密码必须一致
3. 新密码长度必须在6-20位之间

### 3. 密码重置（未来功能）

管理员可以重置用户的密码，重置后的密码为 **123456**。

## 数据库设计

### user_credentials 表结构

```sql
CREATE TABLE `user_credentials` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `student_id` BIGINT DEFAULT NULL COMMENT '学生ID（当role=STUDENT时）',
  `teacher_id` BIGINT DEFAULT NULL COMMENT '教师ID（当role=TEACHER时）',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名（学号/工号/管理员账号）',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希值（BCrypt）',
  `email` VARCHAR(100) DEFAULT NULL,
  `phone` VARCHAR(20) DEFAULT NULL,
  `role` VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT '角色：ADMIN, TEACHER, STUDENT',
  `last_login_time` DATETIME DEFAULT NULL,
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_student_id` (`student_id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 实现要点

### 1. 创建学生时的密码处理

```java
// 创建登录凭证（初始密码：123456，由管理员设定）
String defaultPassword = "123456";
String passwordHash = passwordEncoder.encode(defaultPassword);
userCredentialMapper.insertCredential(
    student.getStudentId(),
    null, // teacher_id为NULL（学生）
    dto.getUsername(),
    passwordHash,
    dto.getEmail(),
    dto.getPhone(),
    "STUDENT"
);
```

### 2. 创建教师时的密码处理

教师创建时也需要在 `user_credentials` 表中创建登录凭证：

```java
// 在teacher-service中创建教师后，需要调用user-service创建登录凭证
// 或者通过消息队列通知user-service创建
```

### 3. 权限验证

所有创建、修改、删除用户的接口都需要验证管理员权限：

```java
if (!StringUtils.hasText(role) || !Constants.ROLE_ADMIN.equals(role)) {
    throw new BusinessException(403, "只有管理员可以执行此操作");
}
```

## 安全建议

1. **强制首次登录修改密码**：用户首次登录后，系统应提示用户修改密码
2. **密码复杂度要求**：建议新密码包含字母、数字，长度至少6位
3. **密码过期策略**：可以设置密码过期时间，要求用户定期修改密码
4. **登录失败锁定**：连续多次登录失败后，临时锁定账号
5. **操作日志**：记录所有用户创建、修改、删除操作，便于审计

## 总结

- ✅ 只有管理员可以创建用户（包括教师和学生）
- ✅ 所有用户的初始密码都是 **123456**
- ✅ 所有用户都可以修改自己的密码
- ✅ 登录凭证统一存储在 `user_service_db.user_credentials` 表中
- ✅ 所有创建用户的接口都有管理员权限验证

