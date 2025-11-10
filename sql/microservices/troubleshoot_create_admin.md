# 排查创建admin用户的问题

## 错误信息

```
500 系统异常：No static resource auth/create-admin.
```

## 可能的原因

1. **应用没有正确启动**
   - 检查auth-service是否正在运行
   - 检查端口8081是否被占用
   - 查看控制台是否有启动错误

2. **Controller没有被正确扫描**
   - 检查`@RestController`和`@RequestMapping`注解是否正确
   - 检查`@ComponentScan`是否包含Controller包

3. **路由配置问题**
   - 检查SecurityConfig是否正确配置了`/auth/**`的访问权限

## 解决方案

### 方案1：检查应用是否正在运行

1. 查看控制台输出，确认应用已启动
2. 检查是否有错误信息
3. 确认端口8081是否被占用

### 方案2：使用POST方法而不是GET方法

尝试使用POST方法：

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/auth/create-admin?password=123456" -Method POST
```

### 方案3：检查应用日志

查看应用启动日志，确认：
- Controller是否被正确扫描
- 路由是否正确注册
- 是否有异常信息

### 方案4：直接使用浏览器访问

在浏览器地址栏输入：

```
http://localhost:8081/auth/create-admin?password=123456
```

### 方案5：检查SecurityConfig配置

确认SecurityConfig中已配置：

```java
.requestMatchers("/auth/**").permitAll()
```

### 方案6：重启应用

如果应用正在运行，尝试重启应用：
1. 停止应用
2. 重新启动应用
3. 等待应用完全启动
4. 再次尝试创建admin用户

## 验证应用是否正常运行

### 检查1：访问健康检查端点（如果有）

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/actuator/health" -Method GET
```

### 检查2：访问登录接口（测试路由是否正常）

```powershell
$body = @{
    username = "test"
    password = "test"
    type = "ADMIN"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/auth/login" -Method POST -Body $body -ContentType "application/json"
```

如果登录接口返回"用户名或密码错误"，说明路由是正常的，只是用户不存在。

## 如果以上方法都不行

1. **检查应用启动日志**，查看是否有Controller注册信息
2. **检查端口是否正确**，确认应用运行在8081端口
3. **检查防火墙设置**，确保端口8081没有被阻止
4. **尝试使用其他工具**，如Postman或浏览器

