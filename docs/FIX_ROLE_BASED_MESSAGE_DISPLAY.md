# 修复：按角色发送消息的接收者显示

## 问题描述

管理员发送"仅教师"或"仅学生"通知后：
1. ❌ 教师/学生看不到消息
2. ❌ 消息管理中接收者显示为空

## 问题原因

### 问题1: 前端过滤掉了平台公告
**位置**: 
- `frontend-teacher/src/views/MessageCenter.vue`
- `frontend-student/src/views/MessageCenter.vue`

**原因**: 前端代码错误地过滤掉了所有 `PLATFORM_ANNOUNCEMENT` 类型的消息

### 问题2: 接收者名称未设置
**位置**: `message-service/.../MessageService.java`

**原因**: 对于全局公告，没有设置 `receiverName` 字段

## 完整解决方案

### 修复1: 前端显示逻辑

#### 教师端 (frontend-teacher/src/views/MessageCenter.vue)

**修改前**:
```javascript
// ❌ 错误：过滤掉所有平台公告
receivedMessages.value = records.filter(r => 
  !(r.scopeType === 'GLOBAL' || r.messageType === 'PLATFORM_ANNOUNCEMENT')
)
receivedTotal.value = receivedMessages.value.length
```

**修改后**:
```javascript
// ✅ 正确：显示所有消息（后端已通过 roleMask 控制可见性）
receivedMessages.value = records
receivedTotal.value = response.data.data.total || 0
```

#### 学生端 (frontend-student/src/views/MessageCenter.vue)

**修改前**:
```javascript
// ❌ 错误：过滤掉所有平台公告
tableData.value = records.filter(r => 
  !(r.scopeType === 'GLOBAL' || r.messageType === 'PLATFORM_ANNOUNCEMENT')
)
total.value = tableData.value.length
```

**修改后**:
```javascript
// ✅ 正确：显示所有消息（后端已通过 roleMask 控制可见性）
tableData.value = records
total.value = response.data.data.total || 0
```

### 修复2: 后端接收者名称设置

#### MessageService.java

**修改位置**: `fillUserNames` 方法

**添加逻辑**:
```java
// 全局公告：根据 roleMask 设置接收者名称
else if ("GLOBAL".equals(dto.getScopeType())) {
    String roleMask = dto.getRoleMask();
    if (StringUtils.hasText(roleMask)) {
        if (roleMask.equals("TEACHER")) {
            dto.setReceiverName("全体教师");
        } else if (roleMask.equals("STUDENT")) {
            dto.setReceiverName("全体学生");
        } else if (roleMask.contains("ADMIN") && roleMask.contains("TEACHER") && roleMask.contains("STUDENT")) {
            dto.setReceiverName("全体用户");
        } else {
            dto.setReceiverName("指定用户组");
        }
    } else {
        dto.setReceiverName("全体用户");
    }
}
```

## 修复效果

### 修复前

#### 管理员端消息管理
```
发送者    | 接收者 | 内容
管理员    | (空)   | 教师会议通知
```

#### 教师端消息中心
```
(没有消息显示)
```

### 修复后

#### 管理员端消息管理
```
发送者    | 接收者   | 内容
管理员    | 全体教师 | 教师会议通知
管理员    | 全体学生 | 选课通知
管理员    | 全体用户 | 系统维护通知
```

#### 教师端消息中心
```
发送者    | 内容
管理员    | 教师会议通知
管理员    | 系统维护通知
```

#### 学生端消息中心
```
发送者    | 内容
管理员    | 选课通知
管理员    | 系统维护通知
```

## 接收者名称映射规则

| roleMask | 显示名称 |
|----------|----------|
| `TEACHER` | 全体教师 |
| `STUDENT` | 全体学生 |
| `ADMIN,TEACHER,STUDENT` | 全体用户 |
| 其他组合 | 指定用户组 |
| 空或null | 全体用户 |

## 完整的消息流程

### 1. 管理员发送"仅教师"通知

```
前端发送:
{
  messageType: "PLATFORM_ANNOUNCEMENT",
  scopeType: "GLOBAL",
  content: "教师会议通知",
  roleMask: "TEACHER"
}

后端处理:
1. 保存消息到数据库
2. fillUserNames() 设置:
   - senderName: "管理员"
   - receiverName: "全体教师"
3. 返回完整的 MessageDTO

前端显示:
- 管理员端: 发送者=管理员, 接收者=全体教师
- 教师端: 可以看到消息
- 学生端: 看不到消息（后端权限过滤）
```

### 2. 教师查看消息

```
教师请求:
GET /api/message/page
Headers:
  X-User-Id: 2
  X-Role: TEACHER

后端处理:
1. 查询数据库
2. 权限过滤:
   - 检查 roleMask 是否包含 "TEACHER"
   - 包含则返回，否则过滤掉
3. fillUserNames() 填充名称
4. 返回消息列表

前端显示:
- 直接显示所有返回的消息
- 不再进行二次过滤
```

## 权限验证流程

### 后端权限验证 (MessagePermissionService.java)

```java
public boolean canReceiveMessage(Long receiverId, String receiverRole, MessageDTO messageDTO) {
    // 管理员可以查看所有消息
    if (Constants.ROLE_ADMIN.equals(receiverRole)) {
        return true;
    }
    
    ScopeType scope = ScopeType.fromCode(messageDTO.getScopeType());
    
    switch (scope) {
        case GLOBAL:
            // 检查 roleMask 是否包含用户角色
            return hasRolePermission(receiverRole, messageDTO.getRoleMask());
        // ... 其他情况
    }
}

private boolean hasRolePermission(String role, String roleMask) {
    if (roleMask == null || roleMask.isEmpty()) {
        return true; // 没有限制，所有人可见
    }
    return roleMask.contains(role); // 检查角色是否在 roleMask 中
}
```

### 前端显示逻辑 (修复后)

```javascript
// 教师端/学生端
const loadReceivedMessages = async () => {
  const response = await api.get('/message/page', { params })
  
  if (response.data.code === 200) {
    const records = response.data.data.records || []
    // ✅ 直接显示，不过滤
    receivedMessages.value = records
    receivedTotal.value = response.data.data.total || 0
  }
}
```

## 测试验证

### 测试步骤

#### 1. 测试"仅教师"通知

```
步骤:
1. 管理员登录
2. 进入消息管理
3. 点击"发送通知"
4. 选择"仅教师"
5. 输入内容: "教师会议通知"
6. 点击发送

验证管理员端:
- 消息管理中应显示:
  发送者: 管理员
  接收者: 全体教师 ✅
  内容: 教师会议通知

验证教师端:
- 登录教师账号
- 进入消息中心
- 应该能看到"教师会议通知" ✅

验证学生端:
- 登录学生账号
- 进入消息中心
- 不应该看到"教师会议通知" ✅
```

#### 2. 测试"仅学生"通知

```
步骤:
1. 管理员发送"仅学生"通知
2. 内容: "选课开始通知"

验证:
- 管理员端接收者显示: 全体学生 ✅
- 学生可以看到 ✅
- 教师看不到 ✅
```

#### 3. 测试"全体用户"通知

```
步骤:
1. 管理员发送"全体用户"通知
2. 内容: "系统维护通知"

验证:
- 管理员端接收者显示: 全体用户 ✅
- 教师可以看到 ✅
- 学生可以看到 ✅
- 管理员可以看到 ✅
```

## 数据库示例

### messages 表数据

```sql
-- 仅教师通知
INSERT INTO messages (
  sender_id, sender_type, receiver_id, receiver_type,
  message_type, scope_type, role_mask,
  content, created_at
) VALUES (
  1, 'ADMIN', NULL, NULL,
  'PLATFORM_ANNOUNCEMENT', 'GLOBAL', 'TEACHER',
  '教师会议通知', NOW()
);

-- 仅学生通知
INSERT INTO messages (
  sender_id, sender_type, receiver_id, receiver_type,
  message_type, scope_type, role_mask,
  content, created_at
) VALUES (
  1, 'ADMIN', NULL, NULL,
  'PLATFORM_ANNOUNCEMENT', 'GLOBAL', 'STUDENT',
  '选课开始通知', NOW()
);

-- 全体用户通知
INSERT INTO messages (
  sender_id, sender_type, receiver_id, receiver_type,
  message_type, scope_type, role_mask,
  content, created_at
) VALUES (
  1, 'ADMIN', NULL, NULL,
  'PLATFORM_ANNOUNCEMENT', 'GLOBAL', 'ADMIN,TEACHER,STUDENT',
  '系统维护通知', NOW()
);
```

### 查询示例

```sql
-- 查看所有全局公告
SELECT 
  message_id,
  sender_id,
  message_type,
  scope_type,
  role_mask,
  content,
  created_at
FROM messages
WHERE scope_type = 'GLOBAL'
ORDER BY created_at DESC;

-- 查看仅教师的通知
SELECT * FROM messages 
WHERE scope_type = 'GLOBAL' 
  AND role_mask = 'TEACHER';

-- 查看仅学生的通知
SELECT * FROM messages 
WHERE scope_type = 'GLOBAL' 
  AND role_mask = 'STUDENT';
```

## 注意事项

1. **前端修改无需重启** - 刷新页面即可
2. **后端修改需要重启** - 重启 message-service
3. **权限控制在后端** - 前端只负责展示
4. **接收者名称自动生成** - 根据 roleMask 自动设置
5. **管理员可见所有** - 管理员可以看到所有通知（监控用）

## 相关文件

### 前端文件
- `frontend-admin/src/views/MessageManagement.vue` - 管理员发送通知
- `frontend-teacher/src/views/MessageCenter.vue` - 教师查看消息
- `frontend-student/src/views/MessageCenter.vue` - 学生查看消息

### 后端文件
- `message-service/.../MessageService.java` - 消息服务
- `message-service/.../MessagePermissionService.java` - 权限验证
- `message-service/.../MessageController.java` - 消息接口

## 更新日志

**版本**: 1.2.2  
**日期**: 2024-11-11  
**问题**: 
1. 教师/学生看不到按角色发送的通知
2. 消息管理中接收者显示为空

**修复**: 
1. 移除前端的平台公告过滤逻辑
2. 添加后端接收者名称自动设置逻辑

**影响**: 
- 前端: 教师端、学生端消息中心
- 后端: MessageService.fillUserNames 方法

**测试**: 已验证三种发送范围都能正常工作
