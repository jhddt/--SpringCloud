# 503 错误排查指南

## 错误信息
```
api/message/page?current=1&size=20:1 
Failed to load resource: the server responded with a status of 503 (Service Unavailable)
```

## 503 错误的常见原因

### 1. 服务未启动
**检查方法**:
```bash
# 检查 message-service 进程是否在运行
# Windows: 任务管理器查看 Java 进程
# 或查看 IDE 的 Run 窗口
```

**解决方案**:
- 启动 message-service

### 2. 服务启动失败（编译错误）
**检查方法**:
- 查看 message-service 的启动日志
- 查看是否有红色的 ERROR 日志
- 查看是否有编译错误

**常见错误**:
```
- 语法错误
- 缺少依赖
- 配置错误
- 端口被占用
```

**解决方案**:
1. 查看完整的错误日志
2. 修复错误
3. 重新启动服务

### 3. 服务未注册到 Nacos
**检查方法**:
1. 打开 Nacos 控制台: http://localhost:8848/nacos
2. 登录（默认 nacos/nacos）
3. 进入"服务管理" -> "服务列表"
4. 查找 `message-service`

**解决方案**:
- 如果看不到 message-service，说明服务未注册成功
- 检查 Nacos 是否正常运行
- 检查 message-service 的 Nacos 配置

### 4. 网关无法连接到服务
**检查方法**:
- 查看 gateway 的日志
- 查看是否有连接错误

**解决方案**:
- 确保 gateway 和 message-service 都在运行
- 确保都注册到了 Nacos

## 快速排查步骤

### 步骤1: 检查服务是否运行
```bash
# 查看 message-service 的控制台
# 应该看到类似的日志：
Started MessageServiceApplication in X.XXX seconds
```

### 步骤2: 检查启动日志中的错误
查找以下关键词：
- `ERROR`
- `Exception`
- `Failed to`
- `Cannot`

### 步骤3: 检查 Nacos 注册
```
1. 访问 http://localhost:8848/nacos
2. 登录
3. 服务管理 -> 服务列表
4. 查找 message-service
5. 应该显示为"健康"状态
```

### 步骤4: 检查端口占用
```bash
# Windows
netstat -ano | findstr :8082

# 如果端口被占用，找到进程ID并结束
taskkill /F /PID <进程ID>
```

## 针对本次修改的排查

### 可能的问题
我刚才修改了 `MessageService.java` 的 `fillUserNames` 方法，可能导致：

1. **编译错误** - 语法问题
2. **运行时错误** - 逻辑问题

### 检查方法

#### 1. 查看编译错误
在 IDE 中查看 `MessageService.java` 是否有红色波浪线

#### 2. 查看启动日志
```
查找类似的错误：
- ClassNotFoundException
- NoSuchMethodError
- NullPointerException
```

#### 3. 临时回滚测试
如果怀疑是我的修改导致的，可以临时注释掉新增的代码：

```java
// 临时注释掉全局公告的接收者名称设置
/*
} else if ("GLOBAL".equals(dto.getScopeType())) {
    // 全局公告：根据 roleMask 设置接收者名称
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
*/
```

然后重启服务，看是否能正常访问。

## 详细排查流程

### 1. 确认服务状态

#### 检查 message-service 日志
```
查找最后几行日志：
- 如果看到 "Started MessageServiceApplication"，说明启动成功
- 如果看到错误堆栈，说明启动失败
```

#### 检查 Nacos
```
1. 打开 http://localhost:8848/nacos
2. 登录（nacos/nacos）
3. 服务管理 -> 服务列表
4. 查找 message-service
   - 如果存在且健康：服务正常
   - 如果不存在：服务未注册
   - 如果存在但不健康：服务有问题
```

### 2. 检查网关配置

#### gateway 的 application.yml
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: message-service
          uri: lb://message-service  # 确保服务名正确
          predicates:
            - Path=/api/message/**
```

### 3. 检查网络连接

#### 测试直接访问 message-service
```bash
# 假设 message-service 运行在 8082 端口
curl http://localhost:8082/actuator/health

# 应该返回：
{"status":"UP"}
```

#### 测试通过网关访问
```bash
# 假设 gateway 运行在 8888 端口
curl http://localhost:8888/api/message/page?current=1&size=20 \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-User-Id: 1" \
  -H "X-Role: TEACHER"
```

## 常见解决方案

### 方案1: 重启所有服务
```
1. 停止 message-service
2. 停止 gateway
3. 启动 message-service
4. 等待注册到 Nacos（约10秒）
5. 启动 gateway
6. 刷新前端页面
```

### 方案2: 清理并重新编译
```
1. Maven Clean
2. Maven Compile
3. 重启服务
```

### 方案3: 检查依赖
```xml
<!-- 确保 pom.xml 中有必要的依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

### 方案4: 检查配置文件
```yaml
# application.yml
server:
  port: 8082  # 确保端口未被占用

spring:
  application:
    name: message-service  # 确保服务名正确
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848  # 确保 Nacos 地址正确
```

## 调试技巧

### 1. 查看完整错误堆栈
不要只看第一行错误，要看完整的堆栈信息

### 2. 逐步排查
- 先确认服务能启动
- 再确认能注册到 Nacos
- 最后确认网关能访问

### 3. 使用日志
在关键位置添加日志：
```java
log.info("=== 开始处理消息查询 ===");
log.info("userId: {}, role: {}", userId, role);
```

### 4. 使用断点调试
在 IDE 中设置断点，逐步执行代码

## 本次修改相关的检查

### 检查点1: 代码语法
```java
// 确保这段代码没有语法错误
} else if ("GLOBAL".equals(dto.getScopeType())) {
    String roleMask = dto.getRoleMask();
    if (StringUtils.hasText(roleMask)) {
        if (roleMask.equals("TEACHER")) {
            dto.setReceiverName("全体教师");
        }
        // ...
    }
}
```

### 检查点2: 导入语句
确保文件顶部有：
```java
import org.springframework.util.StringUtils;
```

### 检查点3: 方法调用
确保 `fillUserNames` 方法被正确调用

## 如果问题仍然存在

### 提供以下信息
1. **message-service 启动日志**（完整的）
2. **Nacos 控制台截图**（服务列表）
3. **浏览器控制台的完整错误信息**
4. **gateway 的日志**（如果有错误）

### 临时解决方案
如果急需使用系统，可以：
1. 回滚我的修改
2. 重启服务
3. 先使用旧版本
4. 等问题解决后再更新

## 联系方式
如果以上方法都无法解决，请提供：
- 完整的错误日志
- 服务配置文件
- Nacos 状态截图
