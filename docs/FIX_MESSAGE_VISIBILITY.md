# 修复：教师和学生无法收到平台公告

## 问题描述

管理员发送"仅教师"或"仅学生"通知后，教师和学生在消息中心看不到这些消息。

## 问题原因

前端代码在加载消息时，错误地过滤掉了所有平台公告类型的消息。

### 问题代码

#### 教师端 (`frontend-teacher/src/views/MessageCenter.vue`)
```javascript
// 第435行 - 错误的过滤逻辑
receivedMessages.value = records.filter(r => 
  !(r.scopeType === 'GLOBAL' || r.messageType === 'PLATFORM_ANNOUNCEMENT')
)
```

#### 学生端 (`frontend-student/src/views/MessageCenter.vue`)
```javascript
// 第347行 - 错误的过滤逻辑
tableData.value = records.filter(r => 
  !(r.scopeType === 'GLOBAL' || r.messageType === 'PLATFORM_ANNOUNCEMENT')
)
```

### 问题分析

这段代码的原始意图可能是：
- 不在普通消息列表中显示全局公告
- 在单独的公告区域显示

但实际效果是：
- ❌ 过滤掉了所有 `PLATFORM_ANNOUNCEMENT` 类型的消息
- ❌ 包括"仅教师"和"仅学生"的定向通知
- ❌ 导致用户无法看到管理员发送的通知

## 解决方案

### 修复逻辑

**核心思想**: 
- 后端已经通过 `roleMask` 机制控制了消息的可见性
- 前端不应该再次过滤平台公告
- 直接显示后端返回的所有消息

### 修复后的代码

#### 教师端
```javascript
// 显示所有消息，包括平台公告（后端已通过 roleMask 控制可见性）
receivedMessages.value = records
receivedTotal.value = response.data.data.total || 0
```

#### 学生端
```javascript
// 显示所有消息，包括平台公告（后端已通过 roleMask 控制可见性）
tableData.value = records
total.value = response.data.data.total || 0
```

## 修复效果

### 修复前
```
管理员发送"仅教师"通知
↓
后端正确设置 roleMask = "TEACHER"
↓
教师请求消息列表
↓
后端返回该消息（权限验证通过）
↓
❌ 前端过滤掉平台公告
↓
教师看不到消息
```

### 修复后
```
管理员发送"仅教师"通知
↓
后端正确设置 roleMask = "TEACHER"
↓
教师请求消息列表
↓
后端返回该消息（权限验证通过）
↓
✅ 前端直接显示
↓
教师可以看到消息
```

## 权限控制说明

### 后端权限验证（正确的做法）

```java
// MessagePermissionService.java
public boolean canReceiveMessage(Long receiverId, String receiverRole, MessageDTO messageDTO) {
    // 管理员可以查看所有消息
    if (Constants.ROLE_ADMIN.equals(receiverRole)) {
        return true;
    }
    
    // 检查 roleMask
    if (messageDTO.getScopeType().equals("GLOBAL")) {
        return hasRolePermission(receiverRole, messageDTO.getRoleMask());
    }
    // ...
}

private boolean hasRolePermission(String role, String roleMask) {
    if (roleMask == null || roleMask.isEmpty()) {
        return true;
    }
    return roleMask.contains(role);
}
```

### 前端显示逻辑（修复后）

```javascript
// 直接显示后端返回的消息
// 后端已经过滤掉了用户无权查看的消息
receivedMessages.value = records
```

## 测试验证

### 测试场景1: 仅教师通知

**步骤**:
1. 管理员发送"仅教师"通知
2. 教师登录，进入消息中心
3. 学生登录，进入消息中心

**预期结果**:
- ✅ 教师可以看到该通知
- ❌ 学生看不到该通知
- ✅ 管理员在消息管理中可以看到

### 测试场景2: 仅学生通知

**步骤**:
1. 管理员发送"仅学生"通知
2. 学生登录，进入消息中心
3. 教师登录，进入消息中心

**预期结果**:
- ✅ 学生可以看到该通知
- ❌ 教师看不到该通知
- ✅ 管理员在消息管理中可以看到

### 测试场景3: 全体用户通知

**步骤**:
1. 管理员发送"全体用户"通知
2. 教师登录，进入消息中心
3. 学生登录，进入消息中心

**预期结果**:
- ✅ 教师可以看到该通知
- ✅ 学生可以看到该通知
- ✅ 管理员可以看到该通知

## 数据流程

### 完整的消息可见性控制流程

```
1. 管理员发送通知
   ↓
   设置 messageType = "PLATFORM_ANNOUNCEMENT"
   设置 scopeType = "GLOBAL"
   设置 roleMask = "TEACHER" / "STUDENT" / "ADMIN,TEACHER,STUDENT"
   
2. 消息存入数据库
   ↓
   messages 表中保存 roleMask 字段
   
3. 用户请求消息列表
   ↓
   前端发送请求: GET /api/message/page
   请求头包含: X-User-Id, X-Role
   
4. 后端查询数据库
   ↓
   如果是管理员: 查询所有消息
   如果是普通用户: 查询相关消息
   
5. 后端权限过滤
   ↓
   检查每条消息的 roleMask
   如果 roleMask 包含用户角色: 保留
   否则: 过滤掉
   
6. 返回给前端
   ↓
   前端直接显示（不再二次过滤）
   
7. 用户看到消息
   ↓
   只看到有权限查看的消息
```

## 注意事项

1. **不要在前端过滤权限**
   - 权限控制应该在后端完成
   - 前端只负责展示

2. **信任后端返回的数据**
   - 后端已经做了完整的权限验证
   - 前端不需要再次判断

3. **保持一致性**
   - 教师端和学生端使用相同的逻辑
   - 避免不一致导致的问题

4. **分页数据正确性**
   - 使用 `response.data.data.total` 而不是过滤后的长度
   - 确保分页功能正常工作

## 相关文件

### 修改的文件
- `frontend-teacher/src/views/MessageCenter.vue` (第435-436行)
- `frontend-student/src/views/MessageCenter.vue` (第347-348行)

### 相关后端文件
- `message-service/.../MessageService.java` - 消息查询逻辑
- `message-service/.../MessagePermissionService.java` - 权限验证逻辑

## 更新日志

**版本**: 1.2.1  
**日期**: 2024-11-11  
**问题**: 教师和学生无法收到平台公告  
**修复**: 移除前端的平台公告过滤逻辑  
**影响**: 教师端、学生端消息中心  
**测试**: 已验证三种发送范围都能正常工作
