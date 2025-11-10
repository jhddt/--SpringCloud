# 通过API创建admin用户的方法

## 方法1：使用修改密码接口（如果已登录）

如果系统中已有其他可用的管理员账号，可以先登录，然后使用修改密码接口来重置admin用户的密码。

## 方法2：临时启用注册接口

### 步骤1：修改AuthService.java

临时修改 `register` 方法，允许创建admin用户：

```java
@Transactional
public void register(RegisterDTO registerDTO) {
    // 临时允许创建admin用户
    if (!"admin".equals(registerDTO.getUsername())) {
        throw new BusinessException(403, "学生和教师账号只能由管理员创建，请联系管理员");
    }
    
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(User::getUsername, registerDTO.getUsername());
    User existUser = userMapper.selectOne(wrapper);
    
    if (existUser != null) {
        throw new BusinessException(400, "用户名已存在");
    }
    
    User user = new User();
    user.setUsername(registerDTO.getUsername());
    user.setPasswordHash(passwordEncoder.encode(registerDTO.getPassword())); // 使用passwordHash
    user.setEmail(registerDTO.getEmail());
    user.setPhone(registerDTO.getPhone());
    user.setRole("ADMIN"); // 设置为ADMIN角色
    user.setStatus(1);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    
    userMapper.insert(user);
}
```

### 步骤2：发送注册请求

```bash
POST http://localhost:8081/auth/register
Content-Type: application/json

{
  "username": "admin",
  "password": "123456",
  "email": "admin@edu.cn",
  "phone": "",
  "role": "ADMIN"
}
```

### 步骤3：恢复注册接口

创建完成后，记得恢复注册接口的限制。

## 方法3：使用Java代码直接创建

创建一个临时的main方法或测试类来创建admin用户：

```java
public static void main(String[] args) {
    // 使用BCryptPasswordEncoder生成密码哈希值
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
    String passwordHash = encoder.encode("123456");
    System.out.println("密码哈希值: " + passwordHash);
    System.out.println("长度: " + passwordHash.length());
    
    // 然后使用这个哈希值更新数据库
}
```

