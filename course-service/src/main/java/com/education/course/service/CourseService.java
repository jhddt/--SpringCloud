package com.education.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.education.common.constant.Constants;
import com.education.common.exception.BusinessException;
import com.education.course.dto.CourseDTO;
import com.education.course.entity.Course;
import com.education.course.entity.Teacher;
import com.education.course.mapper.CourseMapper;
import com.education.course.mapper.TeacherMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
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
public class CourseService {
    
    private final CourseMapper courseMapper;
    private final TeacherMapper teacherMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    public CourseDTO getById(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        
        CourseDTO dto = convertToDTO(course);
        
        // 清除缓存并重新设置，确保教师信息是最新的
        String cacheKey = Constants.REDIS_COURSE_PREFIX + id;
        redisTemplate.delete(cacheKey);
        redisTemplate.opsForValue().set(cacheKey, dto, 30, TimeUnit.MINUTES);
        return dto;
    }
    
    public Page<CourseDTO> getPage(Integer current, Integer size, String keyword, Integer status) {
        Page<Course> page = new Page<>(current, size);
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Course::getCourseName, keyword)
                    .or().like(Course::getCourseCode, keyword)
                    .or().like(Course::getTeacherName, keyword));
        }
        
        if (status != null) {
            wrapper.eq(Course::getStatus, status);
        }
        
        wrapper.orderByDesc(Course::getCreatedAt);
        Page<Course> coursePage = courseMapper.selectPage(page, wrapper);
        
        Page<CourseDTO> dtoPage = new Page<>(current, size, coursePage.getTotal());
        List<CourseDTO> dtoList = coursePage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        
        return dtoPage;
    }
    
    @Transactional
    public CourseDTO create(CourseDTO dto) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Course::getCourseCode, dto.getCourseCode());
        Course exist = courseMapper.selectOne(wrapper);
        if (exist != null) {
            throw new BusinessException(400, "课程代码已存在");
        }
        
        // 根据teacherId获取教师信息
        if (dto.getTeacherId() != null) {
            Teacher teacher = teacherMapper.selectById(dto.getTeacherId());
            if (teacher != null) {
                dto.setTeacherName(teacher.getName());
                dto.setDepartment(teacher.getDepartment());
            }
        }
        
        Course course = new Course();
        BeanUtils.copyProperties(dto, course);
        course.setSelectedCount(0);
        course.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        course.setCreatedAt(LocalDateTime.now());
        course.setUpdatedAt(LocalDateTime.now());
        
        courseMapper.insert(course);
        
        CourseDTO result = new CourseDTO();
        BeanUtils.copyProperties(course, result);
        return result;
    }
    
    @Transactional
    public CourseDTO update(Long id, CourseDTO dto) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        
        if (StringUtils.hasText(dto.getCourseCode()) && !dto.getCourseCode().equals(course.getCourseCode())) {
            LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Course::getCourseCode, dto.getCourseCode());
            Course exist = courseMapper.selectOne(wrapper);
            if (exist != null) {
                throw new BusinessException(400, "课程代码已存在");
            }
        }
        
        // 如果teacherId有变化，重新获取教师信息
        if (dto.getTeacherId() != null && !dto.getTeacherId().equals(course.getTeacherId())) {
            Teacher teacher = teacherMapper.selectById(dto.getTeacherId());
            if (teacher != null) {
                dto.setTeacherName(teacher.getName());
                dto.setDepartment(teacher.getDepartment());
            }
        } else if (dto.getTeacherId() != null && dto.getTeacherId().equals(course.getTeacherId())) {
            // 如果teacherId没变，但teacherName可能为空，重新获取
            if (!StringUtils.hasText(dto.getTeacherName())) {
                Teacher teacher = teacherMapper.selectById(dto.getTeacherId());
                if (teacher != null) {
                    dto.setTeacherName(teacher.getName());
                    dto.setDepartment(teacher.getDepartment());
                }
            }
        }
        
        BeanUtils.copyProperties(dto, course, "courseId", "selectedCount", "createdAt");
        course.setUpdatedAt(LocalDateTime.now());
        courseMapper.updateById(course);
        
        redisTemplate.delete(Constants.REDIS_COURSE_PREFIX + id);
        
        CourseDTO result = new CourseDTO();
        BeanUtils.copyProperties(course, result);
        return result;
    }
    
    @Transactional
    public void delete(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        
        courseMapper.deleteById(id);
        redisTemplate.delete(Constants.REDIS_COURSE_PREFIX + id);
    }
    
    /**
     * 将Course实体转换为DTO，并填充教师信息
     */
    private CourseDTO convertToDTO(Course course) {
        CourseDTO dto = new CourseDTO();
        BeanUtils.copyProperties(course, dto);
        
        // 如果teacherId不为空，尝试从教师数据库获取教师信息
        if (dto.getTeacherId() != null) {
            try {
                System.out.println("开始查询教师信息: teacherId=" + dto.getTeacherId());
                Teacher teacher = teacherMapper.selectById(dto.getTeacherId());
                if (teacher != null && StringUtils.hasText(teacher.getName())) {
                    // 总是使用最新的教师名称
                    dto.setTeacherName(teacher.getName());
                    System.out.println("成功填充教师名称: courseId=" + dto.getCourseId() + ", teacherId=" + dto.getTeacherId() + ", teacherName=" + teacher.getName());
                    
                    // 如果department为空，也设置
                    if (!StringUtils.hasText(dto.getDepartment()) && StringUtils.hasText(teacher.getDepartment())) {
                        dto.setDepartment(teacher.getDepartment());
                    }
                } else {
                    System.err.println("警告: 教师不存在或名称为空，teacherId=" + dto.getTeacherId());
                    // 如果查询不到教师，但数据库中teacherName有值，保持原值
                    if (!StringUtils.hasText(dto.getTeacherName())) {
                        dto.setTeacherName("未知教师");
                    }
                }
            } catch (Exception e) {
                // 记录异常但继续处理
                System.err.println("获取教师信息失败: teacherId=" + dto.getTeacherId() + ", error=" + e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
                // 如果查询失败，但数据库中teacherName有值，保持原值
                if (!StringUtils.hasText(dto.getTeacherName())) {
                    dto.setTeacherName("查询失败");
                }
            }
        } else {
            System.err.println("警告: 课程没有关联教师，courseId=" + dto.getCourseId());
            if (!StringUtils.hasText(dto.getTeacherName())) {
                dto.setTeacherName("未分配");
            }
        }
        
        return dto;
    }
}

