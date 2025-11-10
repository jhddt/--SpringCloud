# 密码加密一致性指南

## 问题描述

在微服务架构中，如果不同服务使用不同的密码加密方式或参数，会导致：
- 创建用户时加密的密码无法在登录时验证通过
- 密码验证失败，用户无法登录

## 解决方案

### 1. 统一使用BCryptPasswordEncoder

所有服务（auth-service、student-service、teacher-service）都使用相同的配置：

```java
@Bean
public PasswordEncoder passwordEncoder() {
    // 使用强度10的BCryptPasswordEncoder
    return new BCryptPasswordEncoder(10);
}
```

**重要**：
- 必须指定强度参数为 **10**
- 如果不指定，默认强度可能不同，导致加密结果不一致

### 2. BCrypt密码格式

BCrypt密码哈希值格式：
- 前缀：`$2a$10$`（$2a表示算法版本，10表示强度）
- 长度：**60字符**（固定）
- 示例：`$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO`

### 3. 密码清理

从数据库读取密码时，需要清理可能的额外字符：

```java
String dbPassword = user.getPassword();
dbPassword = dbPassword.trim()
        .replace("\n", "")
        .replace("\r", "")
        .replace("\t", "");
```

### 4. 验证密码格式

在验证密码前，检查格式是否正确：

```java
// BCrypt哈希值应该以$2a$10$开头，长度为60字符
if (!dbPassword.startsWith("$2a$10$") || dbPassword.length() != 60) {
    throw new BusinessException(401, "密码格式错误");
}
```

## 修复已存在的密码数据

### 方法1：使用SQL脚本修复

执行 `sql/microservices/fix_password_encryption.sql` 脚本：

```sql
-- 更新格式错误的密码为正确的BCrypt哈希值
UPDATE `user_credentials` 
SET `password_hash` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO'
WHERE `password_hash` IS NULL 
   OR `password_hash` = '' 
   OR LENGTH(`password_hash`) != 60
   OR `password_hash` NOT LIKE '$2a$10$%';
```

### 方法2：使用Java工具生成

运行 `PasswordUtil.main()` 方法生成新的密码哈希值：

```java
PasswordUtil.main(new String[]{});
// 输出：
// 密码: 123456
// BCrypt哈希值: $2a$10$...
// 验证结果: true
```

### 方法3：重置所有密码

如果需要重置所有用户的密码为默认值"123456"：

```sql
UPDATE `user_credentials`
SET `password_hash` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJ1mO',
    `updated_at` = NOW()
WHERE `role` IN ('STUDENT', 'TEACHER');
```

**注意**：此操作会重置所有用户的密码，用户需要重新登录。

## 验证修复结果

执行以下SQL检查密码格式：

```sql
SELECT 
    `id`,
    `username`,
    `role`,
    LENGTH(`password_hash`) as password_length,
    CASE 
        WHEN `password_hash` LIKE '$2a$10$%' AND LENGTH(`password_hash`) = 60 THEN '格式正确'
        ELSE '格式错误'
    END as password_status
FROM `user_credentials`
ORDER BY `id`;
```

## 常见问题

### Q1: 为什么密码验证失败？

可能的原因：
1. **密码强度不一致**：不同服务使用了不同的BCrypt强度参数
2. **密码格式错误**：数据库中的密码不是标准的BCrypt格式
3. **额外字符**：密码中包含了换行符、空格等额外字符
4. **密码长度错误**：BCrypt哈希值应该是60字符

### Q2: 如何确保密码加密一致性？

1. ✅ 所有服务使用相同的 `BCryptPasswordEncoder(10)`
2. ✅ 创建用户时使用统一的加密方法
3. ✅ 验证密码时清理额外字符
4. ✅ 检查密码格式（长度、前缀）

### Q3: BCrypt每次加密结果都不同，如何验证？

BCrypt每次加密都会生成不同的哈希值，这是正常的。但是：
- 同一个密码的不同哈希值都能通过 `matches()` 方法验证
- 只要使用相同的BCrypt算法和强度，验证就能通过

### Q4: 如何生成新的密码哈希值？

```java
// 方法1：使用PasswordUtil
String hash = PasswordUtil.encode("123456");

// 方法2：直接使用BCryptPasswordEncoder
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
String hash = encoder.encode("123456");
```

## 最佳实践

1. **统一配置**：所有服务使用相同的PasswordEncoder配置
2. **指定强度**：明确指定BCrypt强度为10
3. **清理密码**：从数据库读取时清理额外字符
4. **格式验证**：验证密码格式后再进行匹配
5. **错误处理**：提供清晰的错误信息，便于排查问题

## 相关文件

- `common/src/main/java/com/education/common/util/PasswordUtil.java` - 统一的密码工具类
- `sql/microservices/fix_password_encryption.sql` - 密码修复脚本
- `auth-service/src/main/java/com/education/auth/config/SecurityConfig.java` - 认证服务配置
- `student-service/src/main/java/com/education/student/config/PasswordEncoderConfig.java` - 学生服务配置
- `teacher-service/src/main/java/com/education/teacher/config/PasswordEncoderConfig.java` - 教师服务配置

