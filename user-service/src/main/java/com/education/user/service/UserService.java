package com.education.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.education.common.exception.BusinessException;
import com.education.user.dto.StudentDTO;
import com.education.user.entity.Student;
import com.education.user.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final StudentMapper studentMapper;
    
    public StudentDTO getById(Long id) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            throw new BusinessException(404, "学生不存在");
        }
        
        StudentDTO dto = new StudentDTO();
        BeanUtils.copyProperties(student, dto);
        return dto;
    }
    
    public Page<StudentDTO> getPage(Integer current, Integer size, String keyword, String major, String grade) {
        Page<Student> page = new Page<>(current, size);
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Student::getName, keyword)
                    .or().like(Student::getClassName, keyword));
        }
        
        if (StringUtils.hasText(major)) {
            wrapper.eq(Student::getMajor, major);
        }
        
        if (StringUtils.hasText(grade)) {
            wrapper.eq(Student::getGrade, grade);
        }
        
        wrapper.eq(Student::getStatus, 1); // 只查询启用的学生
        wrapper.orderByDesc(Student::getCreatedAt);
        Page<Student> studentPage = studentMapper.selectPage(page, wrapper);
        
        Page<StudentDTO> dtoPage = new Page<>(current, size, studentPage.getTotal());
        List<StudentDTO> dtoList = studentPage.getRecords().stream()
                .map(student -> {
                    StudentDTO dto = new StudentDTO();
                    BeanUtils.copyProperties(student, dto);
                    return dto;
                })
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        
        return dtoPage;
    }
    
    @Transactional
    public StudentDTO update(Long id, StudentDTO dto) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            throw new BusinessException(404, "学生不存在");
        }
        
        BeanUtils.copyProperties(dto, student, "studentId", "createdAt");
        student.setUpdatedAt(LocalDateTime.now());
        studentMapper.updateById(student);
        
        StudentDTO result = new StudentDTO();
        BeanUtils.copyProperties(student, result);
        return result;
    }
    
    @Transactional
    public void updateStatus(Long id, Integer status) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            throw new BusinessException(404, "学生不存在");
        }
        
        student.setStatus(status);
        student.setUpdatedAt(LocalDateTime.now());
        studentMapper.updateById(student);
    }
    
    @Transactional
    public StudentDTO create(StudentDTO dto) {
        Student student = new Student();
        BeanUtils.copyProperties(dto, student);
        student.setStatus(1);
        student.setCreatedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());
        studentMapper.insert(student);
        
        StudentDTO result = new StudentDTO();
        BeanUtils.copyProperties(student, result);
        return result;
    }
}
