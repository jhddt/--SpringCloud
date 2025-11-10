# 在PowerShell中创建admin用户

## 问题说明

在PowerShell中，`curl`是`Invoke-WebRequest`的别名，不支持`-X`参数。需要使用PowerShell的原生命令。

## 方法1：使用浏览器（最简单，推荐）

直接在浏览器地址栏输入：

```
http://localhost:8081/auth/create-admin?password=123456
```

或者使用默认密码（123456）：

```
http://localhost:8081/auth/create-admin
```

## 方法2：使用PowerShell的Invoke-RestMethod（推荐）

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/auth/create-admin?password=123456" -Method GET
```

或者使用POST方法：

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/auth/create-admin?password=123456" -Method POST
```

## 方法3：使用PowerShell的Invoke-WebRequest

```powershell
Invoke-WebRequest -Uri "http://localhost:8081/auth/create-admin?password=123456" -Method GET
```

或者使用POST方法：

```powershell
Invoke-WebRequest -Uri "http://localhost:8081/auth/create-admin?password=123456" -Method POST
```

## 方法4：使用真正的curl（如果已安装）

如果系统已安装真正的curl（不是PowerShell别名），可以使用：

```powershell
curl.exe -X POST "http://localhost:8081/auth/create-admin?password=123456"
```

或者：

```powershell
curl.exe -X GET "http://localhost:8081/auth/create-admin?password=123456"
```

## 方法5：使用PowerShell的短别名（不推荐）

如果必须使用curl别名，需要先取消别名：

```powershell
Remove-Alias curl -ErrorAction SilentlyContinue
curl -X POST "http://localhost:8081/auth/create-admin?password=123456"
```

## 验证结果

执行成功后，会返回JSON响应：

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

