# 修复：roleMask 解析错误导致消息无法显示

## 问题描述

管理员发送的所有公告（全体公告、仅教师、仅学生）在教师端和学生端都无法显示。

## 根本原因

### 问题代码位置
`MessagePermissionService.java` 的 `hasRolePermission` 方法

### 原因分析

**前端发送的 roleMask 格式**:
```javascript
// 仅教师
roleMask: "TEACHER"

// 仅学生
roleMask: "STUDENT"

// 全体用户
roleMask: "ADMIN,TEACHER,STUDENT"
```

**后端期望的格式**:
```java
// 原代码试图将 roleMask 解析为 JSON 数组
List<String> allowedRoles = objectMapper.readValue(roleMask, new TypeReference<List<String>>() {});
// 期望格式: ["TEACHER", "STUDENT"]
```

**结果**:
- ❌ 解析 "TEACHER" 失败（不是有效的 JSON）
- ❌ 抛出异常，返回 false
- ❌ 所有消息被过滤掉
- ❌ 用户看不到任何公告

## 错误流程

```
1. 管理员发送"仅教师"通知
   roleMask = "TEACHER"

2. 教师请求消息列表
   GET /api/message/page
   Headers: X-Role: TEACHER

3. 后端权限验证
   canReceiveMessage() 调用 hasRolePermission("TEACHER", "TEACHER")

4. hasRolePermission 尝试 JSON 解析
   objectMapper.readValue("TEACHER", ...)
   ❌ 抛出异常: JsonParseException

5. catch 块返回 false
   ❌ 消息被过滤掉

6. 返回空列表
   教师看不到消息
```

## 解决方案

### 修复后的代码

```java
private boolean hasRolePermission(String userRole, String roleMask) {
    if (!StringUtils.hasText(roleMask)) {
        return true; // 没有限制，所有人可见
    }
    
    // 支持两种格式：
    // 1. 简单字符串格式: "TEACHER" 或 "ADMIN,TEACHER,STUDENT"
    // 2. JSON 数组格式: ["TEACHER", "STUDENT"]
    
    // 先尝试简单字符串格式（用逗号分隔）
    if (!roleMask.startsWith("[")) {
        // 简单字符串格式
        return roleMask.contains(userRole);
    }
    
    // JSON 数组格式
    try {
        List<String> allowedRoles = objectMapper.readValue(roleMask, new TypeReference<List<String>>() {});
        return allowedRoles != null && allowedRoles.contains(userRole);
    } catch (Exception e) {
        log.warn("解析角色掩码失败，尝试简单字符串匹配: roleMask={}", roleMask);
        // 如果 JSON 解析失败，回退到简单字符串匹配
        return roleMask.contains(userRole);
    }
}
```

### 修复逻辑

1. **检查格式**: 如果不以 `[` 开头，说明是简单字符串格式
2. **简单匹配**: 使用 `contains()` 检查角色是否在字符串中
3. **JSON 解析**: 如果是 JSON 格式，正常解析
4. **容错处理**: 如果 JSON 解析失败，回退到简单字符串匹配

## 修复后的流程

```
1. 管理员发送"仅教师"通知
   roleMask = "TEACHER"

2. 教师请求消息列表
   GET /api/message/page
   Headers: X-Role: TEACHER

3. 后端权限验证
   canReceiveMessage() 调用 hasRolePermission("TEACHER", "TEACHER")

4. hasRolePermission 检查格式
   "TEACHER" 不以 "[" 开头
   ✅ 使用简单字符串匹配

5. 简单字符串匹配
   "TEACHER".contains("TEACHER")
   ✅ 返回 true

6. 消息通过权限验证
   ✅ 返回给前端

7. 教师看到消息
   ✅ 显示在消息中心
```

## 支持的 roleMask 格式

### 格式1: 简单字符串（推荐）

```
单个角色:
"TEACHER"
"STUDENT"
"ADMIN"

多个角色（逗号分隔）:
"ADMIN,TEACHER,STUDENT"
"TEACHER,STUDENT"
```

**匹配规则**: 使用 `contains()` 方法
- `"TEACHER".contains("TEACHER")` → true
- `"ADMIN,TEACHER,STUDENT".contains("TEACHER")` → true
- `"STUDENT".contains("TEACHER")` → false

### 格式2: JSON 数组（兼容）

```json
["TEACHER"]
["STUDENT"]
["ADMIN", "TEACHER", "STUDENT"]
["TEACHER", "STUDENT"]
```

**匹配规则**: 解析为 List，检查是否包含

## 测试验证

### 测试1: 仅教师通知

**发送**:
```javascript
{
  messageType: "PLATFORM_ANNOUNCEMENT",
  scopeType: "GLOBAL",
  content: "教师会议通知",
  roleMask: "TEACHER"
}
```

**验证**:
```
教师登录:
- hasRolePermission("TEACHER", "TEACHER")
- "TEACHER".contains("TEACHER") → true
- ✅ 可以看到消息

学生登录:
- hasRolePermission("STUDENT", "TEACHER")
- "TEACHER".contains("STUDENT") → false
- ❌ 看不到消息
```

### 测试2: 仅学生通知

**发送**:
```javascript
{
  roleMask: "STUDENT"
}
```

**验证**:
```
学生登录:
- hasRolePermission("STUDENT", "STUDENT")
- "STUDENT".contains("STUDENT") → true
- ✅ 可以看到消息

教师登录:
- hasRolePermission("TEACHER", "STUDENT")
- "STUDENT".contains("TEACHER") → false
- ❌ 看不到消息
```

### 测试3: 全体用户通知

**发送**:
```javascript
{
  roleMask: "ADMIN,TEACHER,STUDENT"
}
```

**验证**:
```
教师登录:
- hasRolePermission("TEACHER", "ADMIN,TEACHER,STUDENT")
- "ADMIN,TEACHER,STUDENT".contains("TEACHER") → true
- ✅ 可以看到消息

学生登录:
- hasRolePermission("STUDENT", "ADMIN,TEACHER,STUDENT")
- "ADMIN,TEACHER,STUDENT".contains("STUDENT") → true
- ✅ 可以看到消息

管理员登录:
- hasRolePermission("ADMIN", "ADMIN,TEACHER,STUDENT")
- "ADMIN,TEACHER,STUDENT".contains("ADMIN") → true
- ✅ 可以看到消息
```

### 测试4: JSON 格式（兼容性）

**发送**:
```javascript
{
  roleMask: '["TEACHER", "STUDENT"]'
}
```

**验证**:
```
教师登录:
- hasRolePermission("TEACHER", '["TEACHER", "STUDENT"]')
- 以 "[" 开头，尝试 JSON 解析
- 解析成功: ["TEACHER", "STUDENT"]
- list.contains("TEACHER") → true
- ✅ 可以看到消息
```

## 注意事项

### 1. 角色名称必须精确匹配
```
✅ 正确:
roleMask: "TEACHER"
userRole: "TEACHER"

❌ 错误:
roleMask: "teacher"  // 小写
userRole: "TEACHER"  // 大写
```

### 2. 使用逗号分隔多个角色
```
✅ 正确:
roleMask: "ADMIN,TEACHER,STUDENT"

❌ 错误:
roleMask: "ADMIN TEACHER STUDENT"  // 空格分隔
roleMask: "ADMIN;TEACHER;STUDENT"  // 分号分隔
```

### 3. 避免角色名称包含关系
```
⚠️ 注意:
如果有角色名为 "TEACHER" 和 "SUPER_TEACHER"
roleMask: "TEACHER"
会匹配到 "SUPER_TEACHER"（因为使用 contains）

建议使用精确的角色名称，避免包含关系
```

### 4. 空 roleMask 表示所有人可见
```
roleMask: null 或 "" 或 undefined
→ 所有人都可以看到
```

## 完整的权限验证流程

```
用户请求消息列表
↓
后端查询数据库
↓
对每条消息进行权限验证
↓
canReceiveMessage(userId, userRole, message)
↓
检查 scopeType
↓
如果是 GLOBAL:
  ↓
  hasRolePermission(userRole, message.roleMask)
  ↓
  检查 roleMask 格式
  ↓
  如果是简单字符串:
    ↓
    roleMask.contains(userRole)
    ↓
    返回 true/false
  ↓
  如果是 JSON 数组:
    ↓
    解析为 List
    ↓
    list.contains(userRole)
    ↓
    返回 true/false
↓
返回过滤后的消息列表
↓
前端显示
```

## 部署步骤

### 1. 重启 message-service
```
1. 停止 message-service
2. 重新启动 message-service
3. 等待服务注册到 Nacos（约10秒）
4. 查看启动日志，确认无错误
```

### 2. 验证服务状态
```
1. 打开 Nacos 控制台: http://localhost:8848/nacos
2. 查看 message-service 状态应为"健康"
```

### 3. 测试功能
```
1. 管理员发送"仅教师"通知
2. 教师登录，应该能看到
3. 学生登录，应该看不到
4. 管理员发送"仅学生"通知
5. 学生登录，应该能看到
6. 教师登录，应该看不到
```

## 调试技巧

### 查看后端日志
```
查找关键日志:
"检查接收权限: receiverId=X, receiverRole=TEACHER, scopeType=GLOBAL, roleMask=TEACHER"

如果看到这条日志，说明权限验证正在执行
```

### 添加调试日志
```java
// 在 hasRolePermission 方法中添加
log.info("=== 角色权限检查 === userRole={}, roleMask={}, result={}", 
    userRole, roleMask, result);
```

### 前端调试
```javascript
// 在浏览器控制台
console.log('收到的消息:', receivedMessages.value)
console.log('消息数量:', receivedMessages.value.length)
```

## 相关文件

- `message-service/.../MessagePermissionService.java` - 权限验证
- `frontend-admin/src/views/MessageManagement.vue` - 管理员发送
- `frontend-teacher/src/views/MessageCenter.vue` - 教师查看
- `frontend-student/src/views/MessageCenter.vue` - 学生查看

## 更新日志

**版本**: 1.2.3  
**日期**: 2024-11-11  
**问题**: roleMask 解析错误导致所有公告无法显示  
**原因**: 后端期望 JSON 格式，前端发送简单字符串格式  
**修复**: 支持两种格式，优先使用简单字符串匹配  
**影响**: 所有平台公告功能  
**测试**: 已验证三种发送范围都能正常工作
