# 快速创建admin用户指南

## 最简单的方法：使用浏览器（GET方法）

直接在浏览器地址栏输入以下URL：

```
http://localhost:8081/auth/create-admin?password=123456
```

或者使用默认密码（123456）：

```
http://localhost:8081/auth/create-admin
```

## 使用curl命令（POST方法）

在命令行中执行：

```bash
curl -X POST "http://localhost:8081/auth/create-admin?password=123456"
```

**注意**：
- 不要包含"POST"在URL中
- URL应该是：`http://localhost:8081/auth/create-admin?password=123456`
- 使用 `-X POST` 参数指定HTTP方法

## 使用Postman

1. 打开Postman
2. 选择 **POST** 或 **GET** 方法
3. 输入URL：`http://localhost:8081/auth/create-admin`
4. 在 **Params** 标签页添加参数：
   - Key: `password`
   - Value: `123456`
5. 点击 **Send** 按钮

## 使用PowerShell（Windows）

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/auth/create-admin?password=123456" -Method GET
```

## 常见错误

### ❌ 错误示例：
```
POST http://localhost:8081/auth/create-admin?password=123456
```
**问题**：包含了"POST"在URL中

### ✅ 正确示例：
```
http://localhost:8081/auth/create-admin?password=123456
```
**说明**：URL中不包含HTTP方法名

## 验证结果

创建成功后，会返回JSON响应：

```json
{
  "code": 200,
  "message": "Admin用户创建成功",
  "data": null
}
```

同时，控制台会输出：

```
==========================================
Admin用户创建成功！
==========================================
用户名: admin
密码: 123456
密码哈希值: $2a$10$...
密码哈希值长度: 60
==========================================
```

## 登录信息

创建成功后，使用以下信息登录：
- 用户名：`admin`
- 密码：`123456`

