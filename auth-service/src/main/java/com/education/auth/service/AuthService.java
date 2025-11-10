package com.education.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.education.auth.dto.LoginDTO;
import com.education.auth.dto.RegisterDTO;
import com.education.auth.entity.User;
import com.education.auth.mapper.UserMapper;
import com.education.auth.vo.LoginVO;
import com.education.common.constant.Constants;
import com.education.common.exception.BusinessException;
import com.education.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    public LoginVO login(LoginDTO loginDTO) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = userMapper.selectOne(wrapper);
        
        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        
        // 获取并清理数据库中的密码
        String dbPassword = user.getPasswordHash();
        if (dbPassword == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        
        // 清理密码中的空白字符和换行符（防止数据库存储时引入的额外字符）
        // BCrypt标准哈希值长度为60字符，格式：$2a$10$...
        // 注意：如果密码长度超过60，可能是存储时引入了额外字符，需要清理
        
        // 先检查最后一个字符是否是可见字符
        if (dbPassword.length() > 60) {
            System.err.println("DEBUG: 密码哈希值长度超过60，实际长度: " + dbPassword.length());
            System.err.println("DEBUG: 原始密码哈希值: [" + dbPassword + "]");
            // 检查最后一个字符的ASCII码
            char lastChar = dbPassword.charAt(dbPassword.length() - 1);
            System.err.println("DEBUG: 最后一个字符: '" + lastChar + "' (ASCII: " + (int)lastChar + ")");
            
            // 如果最后一个字符是不可见字符（如换行符、回车符等），移除它
            if (lastChar == '\n' || lastChar == '\r' || lastChar == '\t' || lastChar == ' ' || 
                Character.isWhitespace(lastChar) || (int)lastChar < 32) {
                dbPassword = dbPassword.substring(0, dbPassword.length() - 1);
                System.err.println("DEBUG: 移除最后一个不可见字符后的密码哈希值: [" + dbPassword + "]");
            } else {
                // 如果最后一个字符是可见字符，可能是数据库存储时引入了额外字符
                // 尝试截取前60个字符
                dbPassword = dbPassword.substring(0, 60);
                System.err.println("DEBUG: 截取前60个字符后的密码哈希值: [" + dbPassword + "]");
            }
        }
        
        // 清理所有空白字符
        dbPassword = dbPassword.trim()
                .replace("\n", "")
                .replace("\r", "")
                .replace("\t", "")
                .replace(" ", ""); // 清理所有空格
        
        // 验证密码格式：BCrypt哈希值应该以$2a$10$开头，长度为60字符
        // 注意：BCrypt哈希值长度必须严格为60字符
        if (!dbPassword.startsWith("$2a$10$") || dbPassword.length() != 60) {
            // 调试信息：输出实际读取到的密码值
            System.err.println("DEBUG: 密码格式验证失败");
            System.err.println("DEBUG: 密码长度: " + dbPassword.length());
            System.err.println("DEBUG: 密码前缀: " + (dbPassword.length() > 7 ? dbPassword.substring(0, 7) : dbPassword));
            System.err.println("DEBUG: 完整密码哈希值: [" + dbPassword + "]");
            System.err.println("DEBUG: 期望长度: 60, 实际长度: " + dbPassword.length());
            throw new BusinessException(401, "密码格式错误，请联系管理员重置密码");
        }
        
        // 验证密码
        try {
            boolean matches = passwordEncoder.matches(loginDTO.getPassword(), dbPassword);
            if (!matches) {
                System.err.println("DEBUG: 密码验证失败 - 密码不匹配");
                System.err.println("DEBUG: 输入的密码: [" + loginDTO.getPassword() + "]");
                System.err.println("DEBUG: 数据库密码哈希: [" + dbPassword + "]");
                throw new BusinessException(401, "用户名或密码错误");
            }
        } catch (IllegalArgumentException e) {
            // BCrypt格式错误
            System.err.println("DEBUG: BCrypt验证异常: " + e.getMessage());
            System.err.println("DEBUG: 密码哈希值: [" + dbPassword + "]");
            System.err.println("DEBUG: 异常堆栈: ");
            e.printStackTrace();
            throw new BusinessException(401, "密码格式错误，请联系管理员重置密码");
        } catch (Exception e) {
            // 其他异常
            System.err.println("DEBUG: 密码验证异常: " + e.getClass().getName() + " - " + e.getMessage());
            System.err.println("DEBUG: 异常堆栈: ");
            e.printStackTrace();
            throw new BusinessException(401, "用户名或密码错误");
        }
        
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }
        
        // 角色校验
        if ("ADMIN".equals(loginDTO.getType()) && !Constants.ROLE_ADMIN.equals(user.getRole())) {
            throw new BusinessException(403, "无权限访问管理员端");
        }
        
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        
        // 存储token到Redis
        redisTemplate.opsForValue().set(
            Constants.REDIS_TOKEN_PREFIX + user.getId(),
            token,
            24,
            TimeUnit.HOURS
        );
        
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        // TODO: 头像信息需要从students表或teacher表获取，暂时设置为null
        // 如果需要完整功能，可以通过studentId或teacherId关联查询获取avatar_url
        vo.setAvatar(null);
        
        return vo;
    }

    @Transactional
    public void register(RegisterDTO registerDTO) {
        // 安全限制：学生和教师账号只能由管理员创建，注册接口已禁用
        // 如果需要开放注册，可以考虑添加验证码或其他安全机制
        throw new BusinessException(403, "学生和教师账号只能由管理员创建，请联系管理员");
        
        // 以下代码已禁用，保留作为参考
        /*
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, registerDTO.getUsername());
        User existUser = userMapper.selectOne(wrapper);
        
        if (existUser != null) {
            throw new BusinessException(400, "用户名已存在");
        }
        
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setPhone(registerDTO.getPhone());
        user.setRole(Constants.ROLE_STUDENT);
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        
        userMapper.insert(user);
        */
    }
    
    /**
     * 修改密码
     * @param userId 用户ID
     * @param oldPassword 原密码
     * @param newPassword 新密码
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        
        // 验证原密码
        String dbPassword = user.getPasswordHash();
        if (dbPassword != null) {
            dbPassword = dbPassword.trim().replace("\n", "").replace("\r", "").replace("\t", "");
        }
        
        if (!passwordEncoder.matches(oldPassword, dbPassword)) {
            throw new BusinessException(400, "原密码错误");
        }
        
        // 更新密码
        String newPasswordHash = passwordEncoder.encode(newPassword);
        user.setPasswordHash(newPasswordHash);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }
    
    /**
     * 创建或重置admin用户
     * 临时方法，用于创建或重置admin用户
     * @param password 密码（默认：123456）
     */
    @Transactional
    public void createAdminUser(String password) {
        // 删除现有的admin用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, "admin");
        User existUser = userMapper.selectOne(wrapper);
        if (existUser != null) {
            userMapper.delete(wrapper);
        }
        
        // 创建新的admin用户
        User admin = new User();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode(password)); // 使用PasswordEncoder加密
        admin.setRole("ADMIN");
        admin.setStatus(1);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        
        userMapper.insert(admin);
        
        System.out.println("==========================================");
        System.out.println("Admin用户创建成功！");
        System.out.println("==========================================");
        System.out.println("用户名: admin");
        System.out.println("密码: " + password);
        System.out.println("密码哈希值: " + admin.getPasswordHash());
        System.out.println("密码哈希值长度: " + admin.getPasswordHash().length());
        System.out.println("==========================================");
    }
}

