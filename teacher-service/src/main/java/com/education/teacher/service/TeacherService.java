package com.education.teacher.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.education.common.constant.Constants;
import com.education.common.exception.BusinessException;
import com.education.teacher.dto.TeacherDTO;
import com.education.teacher.entity.Teacher;
import com.education.teacher.entity.UserCredential;
import com.education.teacher.mapper.TeacherMapper;
import com.education.teacher.mapper.UserCredentialMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherService {
    
    private final TeacherMapper teacherMapper;
    private final UserCredentialMapper userCredentialMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    
    public TeacherDTO getById(Long id) {
        String cacheKey = Constants.REDIS_TEACHER_PREFIX + id;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            if (cached instanceof TeacherDTO) {
                return (TeacherDTO) cached;
            } else if (cached instanceof LinkedHashMap) {
                return objectMapper.convertValue(cached, TeacherDTO.class);
            }
        }
        
        Teacher teacher = teacherMapper.selectById(id);
        if (teacher == null) {
            throw new BusinessException(404, "教师不存在");
        }
        
        TeacherDTO dto = convertToDTO(teacher);
        
        redisTemplate.opsForValue().set(cacheKey, dto, 30, TimeUnit.MINUTES);
        return dto;
    }
    
    public Page<TeacherDTO> getPage(Integer current, Integer size, String keyword, String department) {
        Page<Teacher> page = new Page<>(current, size);
        LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Teacher::getName, keyword)
                    .or().like(Teacher::getDepartment, keyword)
                    .or().like(Teacher::getTitle, keyword));
        }
        
        if (StringUtils.hasText(department)) {
            wrapper.eq(Teacher::getDepartment, department);
        }
        
        wrapper.eq(Teacher::getStatus, 1); // 只查询启用的教师
        wrapper.orderByDesc(Teacher::getCreatedAt);
        Page<Teacher> teacherPage = teacherMapper.selectPage(page, wrapper);
        
        Page<TeacherDTO> dtoPage = new Page<>(current, size, teacherPage.getTotal());
        List<TeacherDTO> dtoList = teacherPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        
        return dtoPage;
    }
    
    @Transactional
    public TeacherDTO create(TeacherDTO dto) {
        // 验证必填字段
        if (!StringUtils.hasText(dto.getName())) {
            throw new BusinessException(400, "姓名不能为空");
        }
        if (!StringUtils.hasText(dto.getUsername())) {
            throw new BusinessException(400, "工号不能为空");
        }
        
        // 创建教师记录
        Teacher teacher = new Teacher();
        teacher.setName(dto.getName());
        teacher.setGender(dto.getGender());
        teacher.setDepartment(dto.getDepartment());
        teacher.setTitle(dto.getTitle());
        teacher.setAvatarUrl(dto.getAvatarUrl());
        teacher.setStatus(1); // 默认启用
        
        // 构建contactInfo JSON
        if (StringUtils.hasText(dto.getPhone()) || StringUtils.hasText(dto.getEmail())) {
            try {
                String contactInfo = String.format("{\"phone\":\"%s\",\"email\":\"%s\"}", 
                    dto.getPhone() != null ? dto.getPhone() : "", 
                    dto.getEmail() != null ? dto.getEmail() : "");
                teacher.setContactInfo(contactInfo);
            } catch (Exception e) {
                throw new BusinessException(500, "构建联系信息失败");
            }
        }
        
        teacher.setCreatedAt(LocalDateTime.now());
        teacher.setUpdatedAt(LocalDateTime.now());
        
        teacherMapper.insert(teacher);
        
        // 检查用户名（工号）是否已存在
        UserCredential existingCredential = userCredentialMapper.selectByUsername(dto.getUsername());
        if (existingCredential != null) {
            throw new BusinessException(400, "工号已存在");
        }
        
        // 创建登录凭证（初始密码：123456，由管理员设定）
        String defaultPassword = "123456";
        String passwordHash = passwordEncoder.encode(defaultPassword);
        System.out.println("=== 开始创建教师登录凭证 ===");
        System.out.println("教师ID: " + teacher.getTeacherId());
        System.out.println("用户名（工号）: " + dto.getUsername());
        System.out.println("密码哈希值: " + passwordHash);
        System.out.println("密码哈希值长度: " + passwordHash.length());
        System.out.println("邮箱: " + dto.getEmail());
        System.out.println("手机: " + dto.getPhone());
        
        try {
            userCredentialMapper.insertCredential(
                null, // student_id为NULL（教师）
                teacher.getTeacherId(),
                dto.getUsername(),
                passwordHash,
                dto.getEmail(),
                dto.getPhone(),
                "TEACHER"
            );
            System.out.println("插入登录凭证SQL执行完成");
            
            // 验证创建是否成功（在同一事务中查询）
            UserCredential createdCredential = userCredentialMapper.selectByUsername(dto.getUsername());
            if (createdCredential == null) {
                System.err.println("ERROR: 插入后查询不到记录，可能跨数据库操作失败");
                System.err.println("请检查：");
                System.err.println("1. MySQL用户是否有访问user_service_db的权限");
                System.err.println("2. user_service_db.user_credentials表是否存在");
                System.err.println("3. 数据库连接是否正常");
                throw new BusinessException(500, "创建登录凭证失败，插入后查询不到记录。请检查数据库权限和连接。");
            }
            System.out.println("验证成功：登录凭证已创建");
            System.out.println("创建的记录ID: " + createdCredential.getId());
            System.out.println("创建的记录用户名: " + createdCredential.getUsername());
            System.out.println("创建的记录角色: " + createdCredential.getRole());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // 如果创建登录凭证失败，记录错误并抛出异常
            System.err.println("创建教师登录凭证失败: " + e.getClass().getName() + " - " + e.getMessage());
            System.err.println("异常堆栈:");
            e.printStackTrace();
            throw new BusinessException(500, "创建登录凭证失败: " + e.getMessage());
        }
        
        TeacherDTO result = new TeacherDTO();
        BeanUtils.copyProperties(teacher, result);
        // 解析contactInfo
        if (StringUtils.hasText(teacher.getContactInfo())) {
            try {
                String contactInfo = teacher.getContactInfo();
                if (contactInfo.contains("\"phone\"")) {
                    String phone = extractJsonValue(contactInfo, "phone");
                    result.setPhone(phone);
                }
                if (contactInfo.contains("\"email\"")) {
                    String email = extractJsonValue(contactInfo, "email");
                    result.setEmail(email);
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
        }
        return result;
    }
    
    /**
     * 从JSON字符串中提取值（简单实现）
     */
    private String extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return "";
    }
    
    @Transactional
    public TeacherDTO update(Long id, TeacherDTO dto) {
        Teacher teacher = teacherMapper.selectById(id);
        if (teacher == null) {
            throw new BusinessException(404, "教师不存在");
        }
        
        // 更新教师信息
        if (StringUtils.hasText(dto.getName())) {
            teacher.setName(dto.getName());
        }
        if (StringUtils.hasText(dto.getGender())) {
            teacher.setGender(dto.getGender());
        }
        if (StringUtils.hasText(dto.getDepartment())) {
            teacher.setDepartment(dto.getDepartment());
        }
        if (StringUtils.hasText(dto.getTitle())) {
            teacher.setTitle(dto.getTitle());
        }
        if (StringUtils.hasText(dto.getAvatarUrl())) {
            teacher.setAvatarUrl(dto.getAvatarUrl());
        }
        if (dto.getStatus() != null) {
            teacher.setStatus(dto.getStatus());
        }
        
        // 更新联系信息
        if (StringUtils.hasText(dto.getPhone()) || StringUtils.hasText(dto.getEmail())) {
            try {
                String contactInfo = String.format("{\"phone\":\"%s\",\"email\":\"%s\"}", 
                    dto.getPhone() != null ? dto.getPhone() : "", 
                    dto.getEmail() != null ? dto.getEmail() : "");
                teacher.setContactInfo(contactInfo);
            } catch (Exception e) {
                throw new BusinessException(500, "构建联系信息失败");
            }
        }
        
        teacher.setUpdatedAt(LocalDateTime.now());
        teacherMapper.updateById(teacher);
        
        redisTemplate.delete(Constants.REDIS_TEACHER_PREFIX + id);
        
        TeacherDTO result = new TeacherDTO();
        BeanUtils.copyProperties(teacher, result);
        // 解析contactInfo
        if (StringUtils.hasText(teacher.getContactInfo())) {
            try {
                String contactInfo = teacher.getContactInfo();
                if (contactInfo.contains("\"phone\"")) {
                    String phone = extractJsonValue(contactInfo, "phone");
                    result.setPhone(phone);
                }
                if (contactInfo.contains("\"email\"")) {
                    String email = extractJsonValue(contactInfo, "email");
                    result.setEmail(email);
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
        }
        return result;
    }
    
    @Transactional
    public void delete(Long id) {
        Teacher teacher = teacherMapper.selectById(id);
        if (teacher == null) {
            throw new BusinessException(404, "教师不存在");
        }
        
        teacherMapper.deleteById(id);
        redisTemplate.delete(Constants.REDIS_TEACHER_PREFIX + id);
    }
    
    @Transactional
    public void updateStatus(Long id, Integer status) {
        Teacher teacher = teacherMapper.selectById(id);
        if (teacher == null) {
            throw new BusinessException(404, "教师不存在");
        }
        
        teacher.setStatus(status);
        teacher.setUpdatedAt(LocalDateTime.now());
        teacherMapper.updateById(teacher);
        
        redisTemplate.delete(Constants.REDIS_TEACHER_PREFIX + id);
    }
    
    /**
     * 通过userId（user_credentials表的id）获取教师信息
     * 用于教师端获取个人信息
     */
    public TeacherDTO getByUserId(Long userId) {
        // 先查询user_credentials表（位于user_service_db），通过id获取teacher_id
        UserCredential credential = userCredentialMapper.selectByIdFromUserDb(userId);
        if (credential == null) {
            return null;
        }
        
        // 如果teacher_id为null，说明该用户还没有教师记录
        if (credential.getTeacherId() == null) {
            return null;
        }
        
        // 通过teacher_id查询教师信息
        Teacher teacher = teacherMapper.selectById(credential.getTeacherId());
        if (teacher == null) {
            return null;
        }
        
        return convertToDTO(teacher);
    }
    
    /**
     * 将Teacher实体转换为DTO
     */
    private TeacherDTO convertToDTO(Teacher teacher) {
        TeacherDTO dto = new TeacherDTO();
        BeanUtils.copyProperties(teacher, dto);
        
        // 查询工号（username）和userId从user_credentials表
        UserCredential credential = userCredentialMapper.selectByTeacherId(teacher.getTeacherId());
        if (credential != null) {
            dto.setUsername(credential.getUsername());
            dto.setUserId(credential.getId()); // 设置userId，用于消息服务
        }
        
        // 解析contactInfo
        if (StringUtils.hasText(teacher.getContactInfo())) {
            try {
                String contactInfo = teacher.getContactInfo();
                if (contactInfo.contains("\"phone\"")) {
                    String phone = extractJsonValue(contactInfo, "phone");
                    dto.setPhone(phone);
                }
                if (contactInfo.contains("\"email\"")) {
                    String email = extractJsonValue(contactInfo, "email");
                    dto.setEmail(email);
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
        }
        
        return dto;
    }
}
