# 管理员按角色发送通知功能

## 概述
管理员现在可以按照用户角色发送定向通知，支持以下三种发送范围：
1. **全体用户** - 所有人（管理员、教师、学生）都可以看到
2. **仅教师** - 只有教师可以看到
3. **仅学生** - 只有学生可以看到

## 功能说明

### 前端实现

#### 发送通知界面
**位置**: `frontend-admin/src/views/MessageManagement.vue`

**功能**:
- 三个单选按钮选择发送范围
- 实时显示当前选择的范围说明
- 根据选择自动设置 `roleMask` 参数

#### 范围选项

| 选项 | 值 | roleMask | 可见用户 |
|------|------|----------|----------|
| 全体用户 | GLOBAL | ADMIN,TEACHER,STUDENT | 所有人 |
| 仅教师 | TEACHER_ONLY | TEACHER | 只有教师 |
| 仅学生 | STUDENT_ONLY | STUDENT | 只有学生 |

### 后端处理

#### roleMask 机制
消息服务使用 `roleMask` 字段来控制消息的可见性：

```java
// MessagePermissionService.java
private boolean hasRolePermission(String role, String roleMask) {
    if (roleMask == null || roleMask.isEmpty()) {
        return true; // 没有角色限制
    }
    return roleMask.contains(role);
}
```

#### 权限验证流程
1. 用户请求查看消息
2. 系统检查消息的 `roleMask`
3. 如果 `roleMask` 包含用户的角色，则显示消息
4. 否则过滤掉该消息

## 使用方法

### 管理员端操作

#### 1. 发送全体通知
```
1. 登录管理员账号
2. 进入"消息管理"页面
3. 点击"发送通知"按钮
4. 选择"全体用户"
5. 输入通知内容
6. 点击"发送"
```

**效果**: 所有用户（管理员、教师、学生）都能在各自的消息页面看到

#### 2. 发送教师通知
```
1. 登录管理员账号
2. 进入"消息管理"页面
3. 点击"发送通知"按钮
4. 选择"仅教师"
5. 输入通知内容（如：教师会议通知）
6. 点击"发送"
```

**效果**: 
- ✅ 教师可以看到
- ❌ 学生看不到
- ✅ 管理员可以在消息管理中看到（监控用）

#### 3. 发送学生通知
```
1. 登录管理员账号
2. 进入"消息管理"页面
3. 点击"发送通知"按钮
4. 选择"仅学生"
5. 输入通知内容（如：选课通知、考试安排）
6. 点击"发送"
```

**效果**: 
- ✅ 学生可以看到
- ❌ 教师看不到
- ✅ 管理员可以在消息管理中看到（监控用）

### 用户端查看

#### 教师端
**位置**: 教师端 -> 消息中心

可以看到：
- 全体用户通知
- 仅教师通知
- 自己的私聊消息
- 课程相关消息

#### 学生端
**位置**: 学生端 -> 消息中心

可以看到：
- 全体用户通知
- 仅学生通知
- 自己的私聊消息
- 课程相关消息

## 技术实现

### 前端发送逻辑

```javascript
// 根据选择的范围设置 roleMask
let roleMask = ''
let actualScopeType = 'GLOBAL'

if (sendForm.value.scopeType === 'TEACHER_ONLY') {
  roleMask = 'TEACHER'
  actualScopeType = 'GLOBAL'
} else if (sendForm.value.scopeType === 'STUDENT_ONLY') {
  roleMask = 'STUDENT'
  actualScopeType = 'GLOBAL'
} else {
  // GLOBAL - 所有人可见
  roleMask = 'ADMIN,TEACHER,STUDENT'
  actualScopeType = 'GLOBAL'
}

const response = await api.post('/message/send', {
  messageType: 'PLATFORM_ANNOUNCEMENT',
  scopeType: actualScopeType,
  contentType: 'TEXT',
  content: sendForm.value.content,
  roleMask: roleMask
})
```

### 后端权限验证

```java
// MessagePermissionService.java
public boolean canReceiveMessage(Long receiverId, String receiverRole, MessageDTO messageDTO) {
    // 管理员可以查看所有消息
    if (Constants.ROLE_ADMIN.equals(receiverRole)) {
        return true;
    }
    
    ScopeType scope = ScopeType.fromCode(messageDTO.getScopeType());
    
    switch (scope) {
        case GLOBAL:
            // 检查角色权限
            return hasRolePermission(receiverRole, messageDTO.getRoleMask());
        // ... 其他情况
    }
}

private boolean hasRolePermission(String role, String roleMask) {
    if (roleMask == null || roleMask.isEmpty()) {
        return true;
    }
    return roleMask.contains(role);
}
```

## 数据库结构

### messages 表
```sql
CREATE TABLE messages (
  message_id BIGINT PRIMARY KEY,
  sender_id BIGINT,
  receiver_id BIGINT,
  message_type VARCHAR(50),
  scope_type VARCHAR(50),
  scope_id BIGINT,
  role_mask VARCHAR(255),  -- 角色掩码，如 "TEACHER" 或 "ADMIN,TEACHER,STUDENT"
  content TEXT,
  -- ... 其他字段
);
```

### 示例数据

#### 全体通知
```sql
INSERT INTO messages (message_type, scope_type, role_mask, content) 
VALUES ('PLATFORM_ANNOUNCEMENT', 'GLOBAL', 'ADMIN,TEACHER,STUDENT', '系统维护通知');
```

#### 教师通知
```sql
INSERT INTO messages (message_type, scope_type, role_mask, content) 
VALUES ('PLATFORM_ANNOUNCEMENT', 'GLOBAL', 'TEACHER', '教师培训通知');
```

#### 学生通知
```sql
INSERT INTO messages (message_type, scope_type, role_mask, content) 
VALUES ('PLATFORM_ANNOUNCEMENT', 'GLOBAL', 'STUDENT', '选课开始通知');
```

## 使用场景

### 场景1: 教师会议通知
```
管理员发送"仅教师"通知：
"各位老师，本周五下午3点召开教学研讨会，请准时参加。"

结果：
- 所有教师都能看到
- 学生看不到
- 不会干扰学生的消息列表
```

### 场景2: 选课通知
```
管理员发送"仅学生"通知：
"2024春季学期选课系统已开放，请同学们及时选课。"

结果：
- 所有学生都能看到
- 教师看不到
- 针对性强，信息精准送达
```

### 场景3: 系统维护通知
```
管理员发送"全体用户"通知：
"系统将于今晚22:00-23:00进行维护，期间无法访问。"

结果：
- 所有用户都能看到
- 确保信息覆盖全面
```

## 优势特点

### 1. 精准推送
- 根据角色定向发送
- 避免信息冗余
- 提高通知效率

### 2. 权限隔离
- 教师通知学生看不到
- 学生通知教师看不到
- 保护信息隐私

### 3. 灵活控制
- 支持多种发送范围
- 管理员可监控所有通知
- 便于信息管理

### 4. 用户体验
- 用户只看到相关通知
- 减少信息干扰
- 提升使用体验

## 注意事项

1. **权限验证**: 只有管理员可以发送平台公告
2. **roleMask 格式**: 多个角色用逗号分隔，如 "ADMIN,TEACHER,STUDENT"
3. **管理员特权**: 管理员可以在消息管理中看到所有通知（包括仅教师/仅学生的）
4. **消息类型**: 统一使用 "PLATFORM_ANNOUNCEMENT" 类型
5. **范围类型**: 统一使用 "GLOBAL"，通过 roleMask 控制可见性

## 测试验证

### 测试步骤

#### 1. 测试仅教师通知
```
1. 管理员发送"仅教师"通知
2. 教师账号登录，应该能看到
3. 学生账号登录，应该看不到
4. 管理员在消息管理中应该能看到
```

#### 2. 测试仅学生通知
```
1. 管理员发送"仅学生"通知
2. 学生账号登录，应该能看到
3. 教师账号登录，应该看不到
4. 管理员在消息管理中应该能看到
```

#### 3. 测试全体通知
```
1. 管理员发送"全体用户"通知
2. 教师账号登录，应该能看到
3. 学生账号登录，应该能看到
4. 管理员账号登录，应该能看到
```

## 问题排查

### 问题1: 教师/学生看不到通知
**检查**:
1. roleMask 是否正确设置
2. 用户角色是否正确
3. 后端日志中的权限验证结果

### 问题2: 所有人都能看到"仅教师"通知
**检查**:
1. 前端是否正确传递 roleMask
2. 后端是否正确验证 roleMask
3. 数据库中 role_mask 字段的值

### 问题3: 管理员看不到某些通知
**检查**:
1. 管理员权限是否正确配置
2. MessagePermissionService 中的管理员判断逻辑

## 更新日志

**版本**: 1.2.0  
**日期**: 2024-11-11  
**更新内容**:
- 添加按角色发送通知功能
- 支持"全体用户"、"仅教师"、"仅学生"三种范围
- 优化发送界面，添加范围说明
- 完善权限验证逻辑
