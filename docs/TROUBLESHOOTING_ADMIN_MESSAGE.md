# 管理员消息权限问题排查指南

## 问题：管理员看不到所有消息

### 排查步骤

#### 1. 检查服务是否重启
```bash
# 确认 message-service 已重启
# 查看启动日志，确认服务正常启动
```

#### 2. 检查用户角色
在浏览器控制台（F12）执行：
```javascript
// 检查当前用户信息
const userStore = useUserStore()
console.log('User ID:', userStore.userId)
console.log('User Role:', userStore.role)
console.log('Username:', userStore.username)

// 应该看到：
// User Role: "ADMIN"
```

或者检查 sessionStorage：
```javascript
console.log('Role in storage:', sessionStorage.getItem('role'))
// 应该输出: "ADMIN"
```

#### 3. 检查网络请求
在浏览器开发者工具的 Network 标签中：

1. 找到 `/api/message/page` 请求
2. 查看 Request Headers：
   ```
   Authorization: Bearer {token}
   X-User-Id: {userId}
   X-Role: ADMIN    <-- 确认这个值是 "ADMIN"
   ```

3. 查看 Response：
   ```json
   {
     "code": 200,
     "data": {
       "records": [...],  // 应该有消息数据
       "total": 10,       // 总数应该大于0
       "size": 20,
       "current": 1
     }
   }
   ```

#### 4. 查看后端日志
在 message-service 的日志中查找：

```
=== 获取消息分页 === userId=1, role=ADMIN, current=1, size=20, ...
✓ 管理员权限：可以查看所有消息
✓ 管理员分支：查询到 X 条消息，总数 Y
✓ 管理员返回：X 条消息
```

**如果看不到这些日志**：
- 服务可能没有重启
- 请求可能没有到达后端

**如果看到 role=null 或其他值**：
- 前端没有正确传递角色信息
- 检查步骤2和3

#### 5. 检查数据库中是否有消息
```sql
USE message_service_db;

-- 查看消息总数
SELECT COUNT(*) FROM messages;

-- 查看最近的消息
SELECT * FROM messages ORDER BY created_at DESC LIMIT 10;
```

如果数据库中没有消息，那是正常的，需要先发送一些消息。

### 常见问题和解决方案

#### 问题1: role 显示为 null
**原因**: 登录后没有正确保存角色信息

**解决**:
1. 退出登录
2. 清除浏览器缓存和 sessionStorage
3. 重新登录
4. 检查登录响应是否包含 role 字段

#### 问题2: role 显示为其他值（如 "TEACHER"）
**原因**: 当前登录的不是管理员账号

**解决**:
1. 使用管理员账号登录
2. 确认数据库中该用户的 role 字段为 "ADMIN"

```sql
-- 检查用户角色
SELECT user_id, username, role FROM users WHERE username = 'admin';

-- 如果需要修改
UPDATE users SET role = 'ADMIN' WHERE username = 'admin';
```

#### 问题3: 请求头中没有 X-Role
**原因**: 前端拦截器配置问题

**解决**:
检查 `frontend-admin/src/utils/api.js`:
```javascript
api.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    // 确保这两行存在
    if (userStore.userId) {
      config.headers['X-User-Id'] = userStore.userId
    }
    if (userStore.role) {
      config.headers['X-Role'] = userStore.role  // <-- 关键
    }
    return config
  }
)
```

#### 问题4: 后端日志显示非管理员分支
**原因**: 角色比较失败

**检查**:
1. Constants.ROLE_ADMIN 的值是否为 "ADMIN"
2. 传入的 role 是否有多余的空格或大小写不匹配

**临时调试**:
在 MessageService.java 的 getPage 方法中添加：
```java
log.info("角色比较: [{}] equals [{}] = {}", 
    currentUserRole, Constants.ROLE_ADMIN, 
    Constants.ROLE_ADMIN.equals(currentUserRole));
```

#### 问题5: 数据库查询返回0条
**原因**: 数据库中确实没有消息

**解决**:
1. 使用教师或学生账号发送一些消息
2. 或者使用管理员发送平台公告
3. 然后刷新消息管理页面

### 完整测试流程

#### 步骤1: 准备测试数据
```bash
# 1. 使用学生账号登录
# 2. 发送一条消息给教师
# 3. 使用教师账号登录
# 4. 回复学生的消息
```

#### 步骤2: 管理员查看
```bash
# 1. 使用管理员账号登录
# 2. 进入"消息管理"页面
# 3. 应该能看到刚才学生和教师之间的消息
```

#### 步骤3: 验证筛选功能
```bash
# 1. 选择"私聊"类型
# 2. 应该只显示私聊消息
# 3. 清除筛选
# 4. 搜索关键词
# 5. 应该显示包含关键词的消息
```

### 调试代码片段

#### 前端调试（浏览器控制台）
```javascript
// 1. 检查用户信息
const userStore = useUserStore()
console.log('User Info:', {
  userId: userStore.userId,
  username: userStore.username,
  role: userStore.role,
  token: userStore.token ? 'exists' : 'missing'
})

// 2. 手动调用API
import api from '@/utils/api'
api.get('/message/page', {
  params: { current: 1, size: 20 }
}).then(res => {
  console.log('Messages:', res.data)
}).catch(err => {
  console.error('Error:', err)
})

// 3. 检查请求头
api.interceptors.request.use(config => {
  console.log('Request Headers:', config.headers)
  return config
})
```

#### 后端调试（MessageService.java）
```java
// 在 getPage 方法开头添加
log.info("=== DEBUG ===");
log.info("currentUserId: {}", currentUserId);
log.info("currentUserRole: [{}]", currentUserRole);
log.info("Constants.ROLE_ADMIN: [{}]", Constants.ROLE_ADMIN);
log.info("equals: {}", Constants.ROLE_ADMIN.equals(currentUserRole));
log.info("=============");
```

### 联系信息

如果以上步骤都无法解决问题，请提供以下信息：

1. **后端日志**（message-service）
   - 包含 "获取消息分页" 的日志
   - 包含角色判断的日志

2. **前端控制台输出**
   - 用户信息
   - 网络请求详情

3. **数据库信息**
   - 消息总数
   - 用户角色

4. **环境信息**
   - 是否重启了服务
   - 浏览器类型和版本
   - 是否清除了缓存

## 快速检查清单

- [ ] message-service 已重启
- [ ] 用户角色为 "ADMIN"
- [ ] 请求头包含 X-Role: ADMIN
- [ ] 后端日志显示"管理员权限"
- [ ] 数据库中有消息数据
- [ ] 浏览器已清除缓存
- [ ] 已重新登录

如果所有项都已确认，但仍然看不到消息，请查看详细的后端日志并联系开发团队。
