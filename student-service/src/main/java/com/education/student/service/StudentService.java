package com.education.student.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.education.common.constant.Constants;
import com.education.common.exception.BusinessException;
import com.education.student.dto.StudentDTO;
import com.education.student.entity.Student;
import com.education.student.entity.UserCredential;
import com.education.student.mapper.StudentMapper;
import com.education.student.mapper.UserCredentialMapper;
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
public class StudentService {
    
    private final StudentMapper studentMapper;
    private final UserCredentialMapper userCredentialMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    
    public StudentDTO getById(Long studentId) {
        String cacheKey = Constants.REDIS_STUDENT_PREFIX + studentId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            if (cached instanceof StudentDTO) {
                return (StudentDTO) cached;
            } else if (cached instanceof LinkedHashMap) {
                return objectMapper.convertValue(cached, StudentDTO.class);
            }
        }
        
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(404, "学生不存在");
        }
        
        StudentDTO dto = convertToDTO(student);
        
        redisTemplate.opsForValue().set(cacheKey, dto, 30, TimeUnit.MINUTES);
        return dto;
    }
    
    public Page<StudentDTO> getPage(Integer current, Integer size, String keyword) {
        Page<Student> page = new Page<>(current, size);
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Student::getName, keyword)
                    .or().like(Student::getMajor, keyword)
                    .or().like(Student::getGrade, keyword));
        }
        
        wrapper.orderByDesc(Student::getCreatedAt);
        Page<Student> studentPage = studentMapper.selectPage(page, wrapper);
        
        Page<StudentDTO> dtoPage = new Page<>(current, size, studentPage.getTotal());
        List<StudentDTO> dtoList = studentPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        
        return dtoPage;
    }
    
    @Transactional
    public StudentDTO create(StudentDTO dto) {
        // 验证必填字段
        if (!StringUtils.hasText(dto.getName())) {
            throw new BusinessException(400, "姓名不能为空");
        }
        if (!StringUtils.hasText(dto.getUsername())) {
            throw new BusinessException(400, "用户名（学号）不能为空");
        }
        
        // 如果传入了userId，说明学生已经登录，需要更新现有的user_credentials记录
        UserCredential existingCredential = null;
        if (dto.getUserId() != null) {
            // 通过userId查找现有的user_credentials记录
            existingCredential = userCredentialMapper.selectById(dto.getUserId());
            if (existingCredential == null) {
                throw new BusinessException(404, "用户凭证不存在");
            }
            // 如果该用户已经有student_id，说明已经创建过学生记录
            if (existingCredential.getStudentId() != null) {
                throw new BusinessException(400, "该用户已经关联了学生记录");
            }
            // 检查用户名（学号）是否已被其他用户使用
            UserCredential credentialByUsername = userCredentialMapper.selectByUsername(dto.getUsername());
            if (credentialByUsername != null && !credentialByUsername.getId().equals(dto.getUserId())) {
                throw new BusinessException(400, "用户名（学号）已被其他用户使用");
            }
        } else {
            // 管理员创建学生：检查用户名是否已存在
            existingCredential = userCredentialMapper.selectByUsername(dto.getUsername());
            if (existingCredential != null) {
                throw new BusinessException(400, "用户名（学号）已存在");
            }
        }
        
        // 创建学生记录
        Student student = new Student();
        student.setName(dto.getName());
        student.setGender(dto.getGender());
        student.setDateOfBirth(dto.getDateOfBirth());
        student.setMajor(dto.getMajor());
        student.setGrade(dto.getGrade());
        student.setClassName(dto.getClassName());
        student.setAvatarUrl(dto.getAvatarUrl());
        student.setStatus(1); // 默认启用
        
        // 构建contactInfo JSON
        if (StringUtils.hasText(dto.getPhone()) || StringUtils.hasText(dto.getEmail())) {
            try {
                String contactInfo = String.format("{\"phone\":\"%s\",\"email\":\"%s\"}", 
                    dto.getPhone() != null ? dto.getPhone() : "", 
                    dto.getEmail() != null ? dto.getEmail() : "");
                student.setContactInfo(contactInfo);
            } catch (Exception e) {
                throw new BusinessException(500, "构建联系信息失败");
            }
        }
        
        student.setCreatedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());
        
        studentMapper.insert(student);
        
        // 如果传入了userId，更新现有的user_credentials记录
        if (dto.getUserId() != null) {
            existingCredential.setStudentId(student.getStudentId());
            existingCredential.setUsername(dto.getUsername());
            if (StringUtils.hasText(dto.getEmail())) {
                existingCredential.setEmail(dto.getEmail());
            }
            if (StringUtils.hasText(dto.getPhone())) {
                existingCredential.setPhone(dto.getPhone());
            }
            existingCredential.setUpdatedAt(LocalDateTime.now());
            userCredentialMapper.updateById(existingCredential);
        } else {
            // 管理员创建学生：创建新的登录凭证（初始密码：123456，由管理员设定）
            String defaultPassword = "123456";
            String passwordHash = passwordEncoder.encode(defaultPassword);
            userCredentialMapper.insertCredential(
                student.getStudentId(),
                null, // teacher_id为NULL（学生）
                dto.getUsername(),
                passwordHash,
                dto.getEmail(),
                dto.getPhone(),
                "STUDENT"
            );
        }
        
        // 清除缓存
        redisTemplate.delete(Constants.REDIS_STUDENT_PREFIX + student.getStudentId());
        
        return convertToDTO(student);
    }
    
    @Transactional
    public StudentDTO update(Long studentId, StudentDTO dto) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(404, "学生不存在");
        }
        
        // 更新学生信息
        if (StringUtils.hasText(dto.getName())) {
            student.setName(dto.getName());
        }
        if (StringUtils.hasText(dto.getGender())) {
            student.setGender(dto.getGender());
        }
        if (dto.getDateOfBirth() != null) {
            student.setDateOfBirth(dto.getDateOfBirth());
        }
        if (StringUtils.hasText(dto.getMajor())) {
            student.setMajor(dto.getMajor());
        }
        if (StringUtils.hasText(dto.getGrade())) {
            student.setGrade(dto.getGrade());
        }
        if (StringUtils.hasText(dto.getClassName())) {
            student.setClassName(dto.getClassName());
        }
        if (StringUtils.hasText(dto.getAvatarUrl())) {
            student.setAvatarUrl(dto.getAvatarUrl());
        }
        if (dto.getStatus() != null) {
            student.setStatus(dto.getStatus());
        }
        
        // 更新联系信息
        if (StringUtils.hasText(dto.getPhone()) || StringUtils.hasText(dto.getEmail())) {
            try {
                String contactInfo = String.format("{\"phone\":\"%s\",\"email\":\"%s\"}", 
                    dto.getPhone() != null ? dto.getPhone() : "", 
                    dto.getEmail() != null ? dto.getEmail() : "");
                student.setContactInfo(contactInfo);
            } catch (Exception e) {
                throw new BusinessException(500, "构建联系信息失败");
            }
        }
        
        student.setUpdatedAt(LocalDateTime.now());
        studentMapper.updateById(student);
        
        // 更新登录凭证（如果需要）
        if (StringUtils.hasText(dto.getEmail()) || StringUtils.hasText(dto.getPhone())) {
            UserCredential credential = userCredentialMapper.selectByStudentId(studentId);
            if (credential != null) {
                if (StringUtils.hasText(dto.getEmail())) {
                    credential.setEmail(dto.getEmail());
                }
                if (StringUtils.hasText(dto.getPhone())) {
                    credential.setPhone(dto.getPhone());
                }
                credential.setUpdatedAt(LocalDateTime.now());
                userCredentialMapper.updateById(credential);
            }
        }
        
        // 清除缓存
        redisTemplate.delete(Constants.REDIS_STUDENT_PREFIX + studentId);
        
        return convertToDTO(student);
    }
    
    @Transactional
    public void delete(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(404, "学生不存在");
        }
        
        // 删除登录凭证（外键约束会自动删除）
        userCredentialMapper.delete(new LambdaQueryWrapper<UserCredential>()
            .eq(UserCredential::getStudentId, studentId));
        
        // 删除学生记录
        studentMapper.deleteById(studentId);
        
        // 清除缓存
        redisTemplate.delete(Constants.REDIS_STUDENT_PREFIX + studentId);
    }
    
    public StudentDTO getByUsername(String username) {
        UserCredential credential = userCredentialMapper.selectByUsername(username);
        if (credential == null) {
            return null;
        }
        
        Student student = studentMapper.selectById(credential.getStudentId());
        if (student == null) {
            return null;
        }
        
        return convertToDTO(student);
    }
    
    /**
     * 通过userId（user_credentials表的id）获取学生信息
     * 用于学生端获取个人信息
     */
    public StudentDTO getByUserId(Long userId) {
        // 先查询user_credentials表，通过id获取student_id
        UserCredential credential = userCredentialMapper.selectById(userId);
        if (credential == null) {
            return null;
        }
        
        // 如果student_id为null，说明该用户还没有学生记录
        if (credential.getStudentId() == null) {
            return null;
        }
        
        // 通过student_id查询学生信息
        Student student = studentMapper.selectById(credential.getStudentId());
        if (student == null) {
            return null;
        }
        
        return convertToDTO(student);
    }
    
    /**
     * 将Student实体转换为DTO
     */
    private StudentDTO convertToDTO(Student student) {
        StudentDTO dto = new StudentDTO();
        dto.setStudentId(student.getStudentId());
        dto.setName(student.getName());
        dto.setGender(student.getGender());
        dto.setDateOfBirth(student.getDateOfBirth());
        dto.setMajor(student.getMajor());
        dto.setGrade(student.getGrade());
        dto.setClassName(student.getClassName());
        dto.setAvatarUrl(student.getAvatarUrl());
        dto.setStatus(student.getStatus());
        
        // 查询学号（username）
        UserCredential credential = userCredentialMapper.selectOne(
            new LambdaQueryWrapper<UserCredential>()
                .eq(UserCredential::getStudentId, student.getStudentId())
        );
        if (credential != null) {
            dto.setUsername(credential.getUsername());
        }
        
        // 解析contactInfo
        if (StringUtils.hasText(student.getContactInfo())) {
            try {
                // 简单的JSON解析（实际可以使用Jackson）
                String contactInfo = student.getContactInfo();
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
    
    /**
     * 从JSON字符串中提取值（简单实现）
     */
    private String extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\":\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return null;
    }
}
