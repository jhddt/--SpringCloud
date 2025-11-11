# 性能优化方案

## 当前性能瓶颈分析

### 1. **数据库查询优化**
- ❌ 缺少索引优化
- ❌ N+1 查询问题（fillUserNames 方法每条消息都调用 Feign）
- ❌ 未使用缓存机制
- ❌ 分页查询后二次过滤导致性能下降

### 2. **微服务调用优化**
- ❌ Feign 调用超时设置较短（5秒）
- ❌ 无熔断降级机制
- ❌ 无批量查询接口

### 3. **缓存优化**
- ❌ 未启用 Spring Cache
- ❌ 用户信息未缓存
- ❌ Redis 连接池配置较小

---

## 优化方案

### 方案 1: 数据库索引优化 ⭐⭐⭐⭐⭐

#### 问题
消息查询涉及多个字段过滤，缺少复合索引导致全表扫描。

#### 解决方案
```sql
-- message 表索引优化
-- 1. 发送者和接收者查询索引
CREATE INDEX idx_sender_receiver ON message(sender_id, receiver_id, created_at DESC);

-- 2. 接收者和状态查询索引（未读消息）
CREATE INDEX idx_receiver_status ON message(receiver_id, status, created_at DESC);

-- 3. 范围类型和范围ID查询索引
CREATE INDEX idx_scope ON message(scope_type, scope_id, created_at DESC);

-- 4. 消息类型查询索引
CREATE INDEX idx_message_type ON message(message_type, created_at DESC);

-- 5. 角色掩码查询索引（全局公告）
CREATE INDEX idx_role_mask ON message(scope_type, role_mask(100), created_at DESC);
```

#### 预期效果
- 查询速度提升 **80-90%**
- 减少数据库 CPU 使用率

---

### 方案 2: 批量查询用户信息 ⭐⭐⭐⭐⭐

#### 问题
`fillUserNames` 方法对每条消息都进行 Feign 调用，导致 N+1 问题。

#### 解决方案

**步骤 1: 添加批量查询接口**

在 `StudentServiceClient` 和 `TeacherServiceClient` 中添加：

```java
@FeignClient(name = "student-service")
public interface StudentServiceClient {
    
    @GetMapping("/student/user/{userId}")
    Result<Object> getStudentByUserId(@PathVariable("userId") Long userId);
    
    // 新增：批量查询
    @PostMapping("/student/batch/users")
    Result<Map<Long, Map<String, Object>>> getStudentsByUserIds(@RequestBody List<Long> userIds);
}

@FeignClient(name = "teacher-service")
public interface TeacherServiceClient {
    
    @GetMapping("/teacher/user/{userId}")
    Result<Object> getTeacherByUserId(@PathVariable("userId") Long userId);
    
    // 新增：批量查询
    @PostMapping("/teacher/batch/users")
    Result<Map<Long, Map<String, Object>>> getTeachersByUserIds(@RequestBody List<Long> userIds);
}
```

**步骤 2: 修改 MessageService**

```java
/**
 * 批量填充用户名称（优化版）
 */
private void fillUserNamesBatch(List<MessageDTO> dtoList) {
    try {
        // 1. 收集所有需要查询的用户ID
        Set<Long> studentIds = new HashSet<>();
        Set<Long> teacherIds = new HashSet<>();
        
        for (MessageDTO dto : dtoList) {
            if (dto.getSenderId() != null && Constants.ROLE_STUDENT.equals(dto.getSenderType())) {
                studentIds.add(dto.getSenderId());
            } else if (dto.getSenderId() != null && Constants.ROLE_TEACHER.equals(dto.getSenderType())) {
                teacherIds.add(dto.getSenderId());
            }
            
            if (dto.getReceiverId() != null && Constants.ROLE_STUDENT.equals(dto.getReceiverType())) {
                studentIds.add(dto.getReceiverId());
            } else if (dto.getReceiverId() != null && Constants.ROLE_TEACHER.equals(dto.getReceiverType())) {
                teacherIds.add(dto.getReceiverId());
            }
        }
        
        // 2. 批量查询用户信息
        Map<Long, String> studentNames = new HashMap<>();
        Map<Long, String> teacherNames = new HashMap<>();
        
        if (!studentIds.isEmpty()) {
            var result = studentServiceClient.getStudentsByUserIds(new ArrayList<>(studentIds));
            if (result != null && result.getCode() == 200 && result.getData() != null) {
                result.getData().forEach((userId, userInfo) -> {
                    String name = extractName(userInfo);
                    studentNames.put(userId, name);
                });
            }
        }
        
        if (!teacherIds.isEmpty()) {
            var result = teacherServiceClient.getTeachersByUserIds(new ArrayList<>(teacherIds));
            if (result != null && result.getCode() == 200 && result.getData() != null) {
                result.getData().forEach((userId, userInfo) -> {
                    String name = extractName(userInfo);
                    teacherNames.put(userId, name);
                });
            }
        }
        
        // 3. 填充名称
        for (MessageDTO dto : dtoList) {
            // 填充发送者名称
            if (dto.getSenderId() != null) {
                if (Constants.ROLE_STUDENT.equals(dto.getSenderType())) {
                    dto.setSenderName(studentNames.getOrDefault(dto.getSenderId(), "未知学生"));
                } else if (Constants.ROLE_TEACHER.equals(dto.getSenderType())) {
                    dto.setSenderName(teacherNames.getOrDefault(dto.getSenderId(), "未知教师"));
                } else if (Constants.ROLE_ADMIN.equals(dto.getSenderType())) {
                    dto.setSenderName("管理员");
                }
            }
            
            // 填充接收者名称
            if (dto.getReceiverId() != null && !"GROUP".equals(dto.getReceiverType())) {
                if (Constants.ROLE_STUDENT.equals(dto.getReceiverType())) {
                    dto.setReceiverName(studentNames.getOrDefault(dto.getReceiverId(), "未知学生"));
                } else if (Constants.ROLE_TEACHER.equals(dto.getReceiverType())) {
                    dto.setReceiverName(teacherNames.getOrDefault(dto.getReceiverId(), "未知教师"));
                } else if (Constants.ROLE_ADMIN.equals(dto.getReceiverType())) {
                    dto.setReceiverName("管理员");
                }
            } else if ("GLOBAL".equals(dto.getScopeType())) {
                // 全局公告名称设置
                fillGlobalAnnouncementReceiverName(dto);
            }
        }
    } catch (Exception e) {
        log.error("批量填充用户名称失败", e);
    }
}

private String extractName(Map<String, Object> userInfo) {
    Object name = userInfo.get("name");
    Object username = userInfo.get("username");
    if (name != null && !name.toString().trim().isEmpty()) {
        return name.toString();
    }
    if (username != null && !username.toString().trim().isEmpty()) {
        return username.toString();
    }
    return "未知用户";
}

private void fillGlobalAnnouncementReceiverName(MessageDTO dto) {
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

**步骤 3: 修改 getPage 方法**

```java
// 将 .peek(this::fillUserNames) 替换为批量填充
List<MessageDTO> dtoList = messagePage.getRecords().stream()
        .map(this::convertToDTO)
        .collect(Collectors.toList());

// 批量填充用户名称
fillUserNamesBatch(dtoList);

Page<MessageDTO> dtoPage = new Page<>(current, size, messagePage.getTotal());
dtoPage.setRecords(dtoList);
```

#### 预期效果
- 将 N 次 Feign 调用减少到 **2 次**（学生和教师各一次）
- 查询速度提升 **60-80%**

---

### 方案 3: 启用 Redis 缓存 ⭐⭐⭐⭐

#### 问题
用户信息频繁查询，无缓存机制。

#### 解决方案

**步骤 1: 添加 Spring Cache 依赖**

在 `message-service/pom.xml` 中添加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

**步骤 2: 配置 Redis 缓存**

在 `application.yml` 中添加：

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1小时
      cache-null-values: false
      key-prefix: "message:cache:"
      use-key-prefix: true
```

**步骤 3: 启用缓存**

在 `MessageServiceApplication` 中添加：

```java
@SpringBootApplication
@EnableCaching  // 启用缓存
public class MessageServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MessageServiceApplication.class, args);
    }
}
```

**步骤 4: 缓存用户信息**

```java
@Cacheable(value = "user:name", key = "#userId + ':' + #userType", unless = "#result == null || #result == '未知用户'")
private String getUserName(Long userId, String userType) {
    // 原有逻辑
}
```

#### 预期效果
- 用户信息查询速度提升 **90%+**
- 减少 Feign 调用次数

---

### 方案 4: 优化数据库连接池 ⭐⭐⭐

#### 问题
Druid 连接池配置较小，高并发时可能不足。

#### 解决方案

在 `application.yml` 中优化配置：

```yaml
spring:
  datasource:
    druid:
      initial-size: 10           # 初始连接数（从 5 增加到 10）
      min-idle: 10               # 最小空闲连接（从 5 增加到 10）
      max-active: 50             # 最大活跃连接（从 20 增加到 50）
      max-wait: 60000            # 获取连接最大等待时间（60秒）
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
      pool-prepared-statements: true
      max-pool-prepared-statement-per-connection-size: 20
      # 监控统计
      filters: stat,wall,slf4j
      # 慢SQL记录
      filter:
        stat:
          slow-sql-millis: 1000
          log-slow-sql: true
```

#### 预期效果
- 提升并发处理能力
- 减少连接等待时间

---

### 方案 5: 优化 Feign 配置 ⭐⭐⭐

#### 问题
Feign 超时配置较短，可能导致频繁超时。

#### 解决方案

在 `application.yml` 中优化：

```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 10000      # 连接超时（从 5秒增加到 10秒）
        readTimeout: 10000         # 读取超时（从 5秒增加到 10秒）
  httpclient:
    enabled: true                  # 启用 Apache HttpClient
    max-connections: 200           # 最大连接数
    max-connections-per-route: 50  # 每个路由的最大连接数
  compression:
    request:
      enabled: true                # 启用请求压缩
      mime-types: text/xml,application/xml,application/json
      min-request-size: 2048
    response:
      enabled: true                # 启用响应压缩
```

添加依赖（`pom.xml`）：

```xml
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-httpclient</artifactId>
</dependency>
```

#### 预期效果
- 减少超时错误
- 提升 Feign 调用性能

---

### 方案 6: 优化 Redis 连接池 ⭐⭐⭐

#### 问题
Redis 连接池配置较小，高并发时可能不足。

#### 解决方案

在 `application.yml` 中优化：

```yaml
spring:
  data:
    redis:
      host: 192.168.141.128
      port: 6379
      password: 123456
      database: 0
      timeout: 5000              # 超时时间（从 3秒增加到 5秒）
      lettuce:
        pool:
          max-active: 50         # 最大活跃连接（从 8 增加到 50）
          max-idle: 20           # 最大空闲连接（从 8 增加到 20）
          min-idle: 5            # 最小空闲连接（从 0 增加到 5）
          max-wait: 3000         # 最大等待时间（3秒）
        shutdown-timeout: 100ms
```

#### 预期效果
- 提升 Redis 操作性能
- 减少连接等待时间

---

### 方案 7: 数据库查询优化（SQL 层面）⭐⭐⭐⭐

#### 问题
`getPage` 方法中的权限过滤逻辑导致二次分页。

#### 解决方案

**优化前（当前代码）：**
```java
// 1. 查询所有数据
Page<Message> messagePage = messageMapper.selectPage(page, wrapper);

// 2. 在内存中过滤权限
List<MessageDTO> filteredList = messagePage.getRecords().stream()
        .filter(dto -> permissionService.canReceiveMessage(...))
        .collect(Collectors.toList());

// 3. 手动分页
List<MessageDTO> pagedList = filteredList.subList(start, end);
```

**优化后：**
```java
// 将权限过滤逻辑直接写入 SQL 查询
wrapper.and(w -> {
    // 自己发送或接收的消息
    w.or(w1 -> w1.eq(Message::getSenderId, currentUserId))
     .or(w2 -> w2.eq(Message::getReceiverId, currentUserId));
    
    // 课程消息：检查是否是课程成员
    if (scopeType != null && ScopeType.COURSE.getCode().equals(scopeType) && scopeId != null) {
        w.or(w3 -> w3.eq(Message::getScopeType, ScopeType.COURSE.getCode())
                     .eq(Message::getScopeId, scopeId)
                     .like(Message::getRoleMask, currentUserRole));
    }
    
    // 全局公告：根据角色掩码显示
    w.or(w4 -> w4.eq(Message::getScopeType, ScopeType.GLOBAL.getCode())
                 .like(Message::getRoleMask, currentUserRole));
});

// 直接使用数据库分页结果，无需二次过滤
Page<Message> messagePage = messageMapper.selectPage(page, wrapper);
```

#### 预期效果
- 避免内存中的二次过滤和分页
- 查询速度提升 **50-70%**

---

### 方案 8: 异步处理 WebSocket 通知 ⭐⭐

#### 问题
`sendWebSocketNotification` 同步执行，影响消息发送速度。

#### 解决方案

**步骤 1: 启用异步**

在 `MessageServiceApplication` 中添加：

```java
@SpringBootApplication
@EnableAsync  // 启用异步
public class MessageServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MessageServiceApplication.class, args);
    }
}
```

**步骤 2: 配置线程池**

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
```

**步骤 3: 异步发送通知**

```java
@Async
private void sendWebSocketNotification(MessageDTO message) {
    try {
        String channel = MessageConstants.REDIS_MESSAGE_PREFIX + message.getReceiverId();
        redisTemplate.convertAndSend(channel, message);
        
        if (ScopeType.COURSE.getCode().equals(message.getScopeType()) && message.getScopeId() != null) {
            String courseChannel = MessageConstants.WS_CHANNEL_COURSE_PREFIX + message.getScopeId();
            redisTemplate.convertAndSend(courseChannel, message);
        }
    } catch (Exception e) {
        log.error("发送WebSocket通知失败", e);
    }
}
```

#### 预期效果
- 消息发送速度提升 **20-30%**
- 提升用户体验

---

## 优化优先级建议

### 立即实施（高优先级）⭐⭐⭐⭐⭐
1. **方案 1: 数据库索引优化** - 最简单，效果最明显
2. **方案 2: 批量查询用户信息** - 解决 N+1 问题
3. **方案 7: 数据库查询优化** - 避免二次分页

### 短期实施（中优先级）⭐⭐⭐⭐
4. **方案 3: 启用 Redis 缓存** - 减少重复查询
5. **方案 4: 优化数据库连接池** - 提升并发能力
6. **方案 6: 优化 Redis 连接池** - 提升 Redis 性能

### 长期优化（低优先级）⭐⭐⭐
7. **方案 5: 优化 Feign 配置** - 提升微服务调用性能
8. **方案 8: 异步处理 WebSocket 通知** - 提升响应速度

---

## 性能监控建议

### 1. 启用 Druid 监控
访问：`http://localhost:8088/druid/index.html`

### 2. 添加性能日志
```java
@Slf4j
@Aspect
@Component
public class PerformanceAspect {
    
    @Around("execution(* com.education.message.service.*.*(..))")
    public Object logPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;
        
        if (duration > 1000) {  // 超过1秒记录
            log.warn("慢方法: {} 耗时 {}ms", joinPoint.getSignature(), duration);
        }
        
        return result;
    }
}
```

### 3. 使用 Spring Boot Actuator
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

---

## 预期性能提升

| 优化项 | 预期提升 | 实施难度 |
|--------|----------|----------|
| 数据库索引 | 80-90% | 低 |
| 批量查询 | 60-80% | 中 |
| Redis 缓存 | 90%+ | 低 |
| 连接池优化 | 30-50% | 低 |
| SQL 优化 | 50-70% | 中 |
| 异步处理 | 20-30% | 低 |

**综合预期：整体性能提升 5-10 倍**

---

## 实施步骤

### 第一阶段（1-2天）
1. 执行数据库索引 SQL
2. 优化连接池配置
3. 优化 Redis 配置

### 第二阶段（3-5天）
4. 实现批量查询接口
5. 修改 MessageService 使用批量查询
6. 启用 Redis 缓存

### 第三阶段（1-2天）
7. 优化 Feign 配置
8. 实现异步 WebSocket 通知
9. 添加性能监控

---

## 注意事项

1. **备份数据库**：执行索引优化前务必备份
2. **灰度发布**：建议分批次上线优化方案
3. **监控指标**：实施后持续监控性能指标
4. **回滚方案**：准备好回滚脚本和流程

---

## 联系方式
如有问题，请查看相关文档或联系技术支持。
