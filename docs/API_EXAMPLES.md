# API 请求示例

## 注册接口

### 使用 Postman 或类似工具

**请求方法**: POST  
**请求 URL**: `http://localhost:8888/api/auth/register`

**请求头 (Headers)**:
```
Content-Type: application/json
Accept: application/json
```

**请求体 (Body)** - 选择 raw，格式选择 JSON:
```json
{
  "username": "admin_new",
  "password": "123456",
  "email": "admin@test.com",
  "phone": "13800138000",
  "role": "ADMIN"
}
```

### 使用 curl 命令

```bash
curl -X POST http://localhost:8888/api/auth/register \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "username": "admin_new",
    "password": "123456",
    "email": "admin@test.com",
    "phone": "13800138000",
    "role": "ADMIN"
  }'
```

### 使用 JavaScript (fetch)

```javascript
fetch('http://localhost:8888/api/auth/register', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  },
  body: JSON.stringify({
    username: 'admin_new',
    password: '123456',
    email: 'admin@test.com',
    phone: '13800138000',
    role: 'ADMIN'
  })
})
.then(response => response.json())
.then(data => console.log(data))
.catch(error => console.error('Error:', error));
```

## 注意事项

1. **Content-Type 必须完整**: 必须是 `application/json`，不能只是 `application`
2. **Accept 头**: 虽然不是必须的，但建议添加 `Accept: application/json`
3. **JSON 格式**: 确保请求体是有效的 JSON 格式
4. **角色字段**: `role` 字段可选，如果不提供，默认为 `STUDENT`

## 常见错误

### 错误: `Invalid mime type "application": does not contain '/'`

**原因**: Content-Type 头不完整，只有 "application" 而没有 "/json"

**解决方法**:
- 确保 Content-Type 头设置为 `application/json`（完整）
- 检查请求工具是否正确设置了请求头
- 如果使用 Postman，确保 Headers 标签页中 Content-Type 的值是 `application/json`

