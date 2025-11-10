# 如何创建admin用户

## 方法1：使用curl命令（推荐）

在命令行中执行：

```bash
curl -X POST "http://localhost:8081/auth/create-admin?password=123456"
```

或者使用POST请求体：

```bash
curl -X POST "http://localhost:8081/auth/create-admin" -d "password=123456"
```

## 方法2：使用Postman

1. 打开Postman
2. 选择 **POST** 方法
3. 输入URL：`http://localhost:8081/auth/create-admin`
4. 在 **Params** 标签页添加参数：
   - Key: `password`
   - Value: `123456`
5. 点击 **Send** 按钮

## 方法3：使用浏览器（临时方法）

如果接口支持GET方法，可以直接在浏览器地址栏输入：

```
http://localhost:8081/auth/create-admin?password=123456
```

**注意**：当前接口是POST方法，浏览器默认是GET，所以这个方法可能不工作。建议使用方法1或方法2。

## 方法4：使用PowerShell（Windows）

```powershell
Invoke-WebRequest -Uri "http://localhost:8081/auth/create-admin?password=123456" -Method POST
```

或者：

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/auth/create-admin?password=123456" -Method POST
```

## 方法5：使用Java代码

如果应用正在运行，可以在代码中直接调用：

```java
@Autowired
private AuthService authService;

public void createAdmin() {
    authService.createAdminUser("123456");
}
```

## 常见错误

### 错误1：Invalid URI
- **原因**：URI格式不正确，可能包含了HTTP方法名
- **解决**：确保URI只包含URL，不包含"POST"等HTTP方法名

### 错误2：Connection refused
- **原因**：应用没有启动或端口不正确
- **解决**：确保auth-service应用正在运行，端口是8081

### 错误3：404 Not Found
- **原因**：URL路径不正确
- **解决**：确保URL是 `http://localhost:8081/auth/create-admin`

## 验证结果

创建成功后，控制台会输出：

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

然后可以使用以下信息登录：
- 用户名：`admin`
- 密码：`123456`

