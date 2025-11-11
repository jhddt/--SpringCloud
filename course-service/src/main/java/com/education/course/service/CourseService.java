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
        // 注意：dto.getTeacherId() 可能是 userId（user_credentials表的id），也可能是真正的teacherId
        String teacherName = null;
        String department = null;
        Long actualTeacherId = dto.getTeacherId();
        
        if (dto.getTeacherId() != null) {
            // 先尝试直接通过teacherId查询（如果已经是teacherId）
            System.out.println("=== 开始查询教师信息 ===");
            System.out.println("输入的teacherId/userId: " + dto.getTeacherId());
            Teacher teacher = teacherMapper.selectById(dto.getTeacherId());
            System.out.println("通过teacherId查询结果: " + (teacher != null ? "找到教师 " + teacher.getName() : "未找到"));
            
            // 如果查询不到，可能是userId，尝试通过userId查询
            if (teacher == null) {
                System.out.println("通过teacherId查询不到教师，尝试通过userId查询: " + dto.getTeacherId());
                // 先尝试分步查询：先获取teacherId
                try {
                    Long teacherIdFromUser = teacherMapper.selectTeacherIdByUserId(dto.getTeacherId());
                    System.out.println("通过userId查询到的teacherId: " + teacherIdFromUser);
                    if (teacherIdFromUser != null) {
                        teacher = teacherMapper.selectById(teacherIdFromUser);
                        System.out.println("通过teacherId查询结果: " + (teacher != null ? "找到教师 " + teacher.getName() : "未找到"));
                        if (teacher != null) {
                            actualTeacherId = teacher.getTeacherId();
                        }
                    } else {
                        System.err.println("警告: userId=" + dto.getTeacherId() + " 在user_credentials表中没有对应的teacher_id");
                    }
                } catch (Exception e) {
                    System.err.println("查询teacherId失败: " + e.getMessage());
                    e.printStackTrace();
                    // 如果分步查询失败，尝试直接JOIN查询
                    try {
                        teacher = teacherMapper.selectByUserId(dto.getTeacherId());
                        System.out.println("通过JOIN查询结果: " + (teacher != null ? "找到教师 " + teacher.getName() : "未找到"));
                        if (teacher != null) {
                            actualTeacherId = teacher.getTeacherId();
                        }
                    } catch (Exception e2) {
                        System.err.println("JOIN查询也失败: " + e2.getMessage());
                    }
                }
                
                if (teacher != null) {
                    // 获取真正的teacherId
                    actualTeacherId = teacher.getTeacherId();
                    System.out.println("通过userId找到教师，真正的teacherId=" + actualTeacherId + ", teacherName=" + teacher.getName());
                } else {
                    System.err.println("错误: 通过userId也查询不到教师，userId=" + dto.getTeacherId());
                    System.err.println("可能的原因：");
                    System.err.println("1. userId在user_credentials表中不存在");
                    System.err.println("2. userId对应的teacher_id为null");
                    System.err.println("3. teacher_id对应的教师在teachers表中不存在");
                    System.err.println("4. 数据库权限问题，无法跨数据库查询");
                }
            } else {
                System.out.println("直接通过teacherId查询成功");
            }
            System.out.println("=== 查询教师信息结束 ===");
            
            if (teacher != null) {
                teacherName = teacher.getName();
                department = teacher.getDepartment();
                System.out.println("创建课程 - 获取教师信息: userId=" + dto.getTeacherId() + ", teacherId=" + actualTeacherId + ", teacherName=" + teacherName + ", department=" + department);
            } else {
                System.err.println("警告: 创建课程时未找到教师信息，userId/teacherId=" + dto.getTeacherId());
                teacherName = "未知教师";
            }
        } else {
            System.err.println("警告: 创建课程时teacherId为空");
            teacherName = "未分配";
        }
        
        // 创建Course实体并设置所有字段
        Course course = new Course();
        course.setCourseName(dto.getCourseName());
        course.setCourseCode(dto.getCourseCode());
        course.setCourseDescription(dto.getCourseDescription());
        course.setCredit(dto.getCredit());
        // 使用实际的teacherId（如果通过userId查询到了真正的teacherId，使用它；否则使用原来的值）
        course.setTeacherId(actualTeacherId != null ? actualTeacherId : dto.getTeacherId());
        course.setTeacherName(teacherName); // 确保设置teacherName
        course.setDepartment(StringUtils.hasText(department) ? department : dto.getDepartment());
        course.setTotalCapacity(dto.getTotalCapacity());
        course.setSelectedCount(0);
        course.setCoverImage(dto.getCoverImage());
        course.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        course.setStartTime(dto.getStartTime());
        course.setEndTime(dto.getEndTime());
        course.setCreatedAt(LocalDateTime.now());
        course.setUpdatedAt(LocalDateTime.now());
        
        // 插入前打印所有字段值
        System.out.println("=== 插入课程前的数据 ===");
        System.out.println("courseId: " + course.getCourseId());
        System.out.println("courseName: " + course.getCourseName());
        System.out.println("courseCode: " + course.getCourseCode());
        System.out.println("teacherId: " + course.getTeacherId());
        System.out.println("teacherName: " + course.getTeacherName());
        System.out.println("department: " + course.getDepartment());
        System.out.println("========================");
        
        // 插入数据库
        int insertResult = courseMapper.insert(course);
        System.out.println("插入结果: " + insertResult + ", 生成的courseId: " + course.getCourseId());
        
        // 插入后立即查询验证
        if (course.getCourseId() != null) {
            Course insertedCourse = courseMapper.selectById(course.getCourseId());
            if (insertedCourse != null) {
                System.out.println("=== 插入后查询的数据 ===");
                System.out.println("courseId: " + insertedCourse.getCourseId());
                System.out.println("teacherId: " + insertedCourse.getTeacherId());
                System.out.println("teacherName: " + insertedCourse.getTeacherName());
                System.out.println("department: " + insertedCourse.getDepartment());
                System.out.println("========================");
                
                // 如果teacherName为空或者是"未知教师"，尝试修复
                if ((!StringUtils.hasText(insertedCourse.getTeacherName()) || "未知教师".equals(insertedCourse.getTeacherName())) 
                    && insertedCourse.getTeacherId() != null) {
                    System.err.println("错误: 插入后teacherName为空或未知，尝试修复... teacherId=" + insertedCourse.getTeacherId());
                    Teacher teacher = teacherMapper.selectById(insertedCourse.getTeacherId());
                    // 如果通过teacherId查询不到，尝试通过userId查询
                    if (teacher == null) {
                        System.out.println("通过teacherId查询不到，尝试通过userId查询: " + insertedCourse.getTeacherId());
                        // 先尝试分步查询
                        try {
                            Long teacherIdFromUser = teacherMapper.selectTeacherIdByUserId(insertedCourse.getTeacherId());
                            System.out.println("通过userId查询到的teacherId: " + teacherIdFromUser);
                            if (teacherIdFromUser != null) {
                                teacher = teacherMapper.selectById(teacherIdFromUser);
                                System.out.println("通过teacherId查询结果: " + (teacher != null ? "找到教师 " + teacher.getName() : "未找到"));
                            }
                        } catch (Exception e) {
                            System.err.println("分步查询失败: " + e.getMessage());
                            e.printStackTrace();
                        }
                        // 如果分步查询失败，尝试JOIN查询
                        if (teacher == null) {
                            try {
                                teacher = teacherMapper.selectByUserId(insertedCourse.getTeacherId());
                            } catch (Exception e) {
                                System.err.println("JOIN查询失败: " + e.getMessage());
                            }
                        }
                    }
                    if (teacher != null) {
                        insertedCourse.setTeacherName(teacher.getName());
                        if (StringUtils.hasText(teacher.getDepartment())) {
                            insertedCourse.setDepartment(teacher.getDepartment());
                        }
                        // 如果通过userId查询到了，更新teacherId为真正的teacherId
                        if (!insertedCourse.getTeacherId().equals(teacher.getTeacherId())) {
                            insertedCourse.setTeacherId(teacher.getTeacherId());
                        }
                        courseMapper.updateById(insertedCourse);
                        System.out.println("修复完成 - teacherId: " + insertedCourse.getTeacherId() + ", teacherName: " + insertedCourse.getTeacherName());
                        // 重新查询
                        insertedCourse = courseMapper.selectById(course.getCourseId());
                    } else {
                        System.err.println("错误: 无法找到教师信息，teacherId/userId=" + insertedCourse.getTeacherId());
                    }
                }
                
                // 使用验证后的数据
                CourseDTO result = convertToDTO(insertedCourse);
                return result;
            }
        }
        
        // 如果查询失败，使用原始数据
        CourseDTO result = convertToDTO(course);
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
        Long actualTeacherId = dto.getTeacherId();
        Teacher teacher = null;
        
        if (dto.getTeacherId() != null) {
            // 先尝试通过teacherId查询
            teacher = teacherMapper.selectById(dto.getTeacherId());
            // 如果查询不到，可能是userId，尝试通过userId查询
            if (teacher == null) {
                teacher = teacherMapper.selectByUserId(dto.getTeacherId());
                // 如果还是查询不到，使用安全的分步查询
                if (teacher == null) {
                    teacher = teacherMapper.selectByUserIdSafe(dto.getTeacherId());
                }
                if (teacher != null) {
                    actualTeacherId = teacher.getTeacherId();
                }
            }
            
            if (teacher != null) {
                dto.setTeacherName(teacher.getName());
                if (StringUtils.hasText(teacher.getDepartment())) {
                    dto.setDepartment(teacher.getDepartment());
                }
            }
        }
        
        // 如果teacherId没变，但teacherName可能为空，重新获取
        if (dto.getTeacherId() != null && dto.getTeacherId().equals(course.getTeacherId())) {
            if (!StringUtils.hasText(course.getTeacherName()) || !StringUtils.hasText(dto.getTeacherName())) {
                if (teacher == null) {
                    teacher = teacherMapper.selectById(dto.getTeacherId());
                    if (teacher == null) {
                        teacher = teacherMapper.selectByUserId(dto.getTeacherId());
                        if (teacher == null) {
                            teacher = teacherMapper.selectByUserIdSafe(dto.getTeacherId());
                        }
                        if (teacher != null) {
                            actualTeacherId = teacher.getTeacherId();
                        }
                    }
                }
                if (teacher != null) {
                    dto.setTeacherName(teacher.getName());
                    if (StringUtils.hasText(teacher.getDepartment())) {
                        dto.setDepartment(teacher.getDepartment());
                    }
                }
            }
        }
        
        // 更新字段，但排除courseId、selectedCount、createdAt
        course.setCourseName(dto.getCourseName());
        course.setCourseCode(dto.getCourseCode());
        course.setCourseDescription(dto.getCourseDescription());
        course.setCredit(dto.getCredit());
        // 使用实际的teacherId
        course.setTeacherId(actualTeacherId != null ? actualTeacherId : dto.getTeacherId());
        // 确保teacherName被设置
        if (StringUtils.hasText(dto.getTeacherName())) {
            course.setTeacherName(dto.getTeacherName());
        } else if (course.getTeacherId() != null) {
            // 如果DTO中没有teacherName，从教师服务获取
            if (teacher == null) {
                teacher = teacherMapper.selectById(course.getTeacherId());
                if (teacher == null) {
                    teacher = teacherMapper.selectByUserId(course.getTeacherId());
                    if (teacher == null) {
                        teacher = teacherMapper.selectByUserIdSafe(course.getTeacherId());
                    }
                }
            }
            if (teacher != null) {
                course.setTeacherName(teacher.getName());
                if (!StringUtils.hasText(course.getDepartment()) && StringUtils.hasText(teacher.getDepartment())) {
                    course.setDepartment(teacher.getDepartment());
                }
            }
        }
        if (StringUtils.hasText(dto.getDepartment())) {
            course.setDepartment(dto.getDepartment());
        }
        course.setTotalCapacity(dto.getTotalCapacity());
        course.setCoverImage(dto.getCoverImage());
        course.setStatus(dto.getStatus());
        course.setStartTime(dto.getStartTime());
        course.setEndTime(dto.getEndTime());
        course.setUpdatedAt(LocalDateTime.now());
        
        System.out.println("=== 更新课程前的数据 ===");
        System.out.println("courseId: " + course.getCourseId());
        System.out.println("teacherId: " + course.getTeacherId());
        System.out.println("teacherName: " + course.getTeacherName());
        System.out.println("========================");
        
        courseMapper.updateById(course);
        
        // 更新后查询验证
        Course updatedCourse = courseMapper.selectById(id);
        if (updatedCourse != null && !StringUtils.hasText(updatedCourse.getTeacherName()) && updatedCourse.getTeacherId() != null) {
            System.err.println("错误: 更新后teacherName为空，尝试修复...");
            Teacher fixTeacher = teacherMapper.selectById(updatedCourse.getTeacherId());
            // 如果通过teacherId查询不到，尝试通过userId查询
            if (fixTeacher == null) {
                fixTeacher = teacherMapper.selectByUserId(updatedCourse.getTeacherId());
                if (fixTeacher == null) {
                    fixTeacher = teacherMapper.selectByUserIdSafe(updatedCourse.getTeacherId());
                }
            }
            if (fixTeacher != null) {
                updatedCourse.setTeacherName(fixTeacher.getName());
                // 如果通过userId查询到了，更新teacherId
                if (!updatedCourse.getTeacherId().equals(fixTeacher.getTeacherId())) {
                    updatedCourse.setTeacherId(fixTeacher.getTeacherId());
                }
                courseMapper.updateById(updatedCourse);
            }
            updatedCourse = courseMapper.selectById(id);
        }
        
        redisTemplate.delete(Constants.REDIS_COURSE_PREFIX + id);
        
        CourseDTO result = convertToDTO(updatedCourse != null ? updatedCourse : course);
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
     * 增加课程选课人数
     */
    @Transactional
    public void incrementSelectedCount(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        
        // 检查容量
        if (course.getSelectedCount() != null && course.getTotalCapacity() != null 
            && course.getSelectedCount() >= course.getTotalCapacity()) {
            throw new BusinessException(400, "课程容量已满");
        }
        
        course.setSelectedCount((course.getSelectedCount() == null ? 0 : course.getSelectedCount()) + 1);
        course.setUpdatedAt(LocalDateTime.now());
        courseMapper.updateById(course);
        
        // 清除缓存
        redisTemplate.delete(Constants.REDIS_COURSE_PREFIX + id);
    }
    
    /**
     * 减少课程选课人数
     */
    @Transactional
    public void decrementSelectedCount(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        
        int currentCount = course.getSelectedCount() == null ? 0 : course.getSelectedCount();
        if (currentCount > 0) {
            course.setSelectedCount(currentCount - 1);
            course.setUpdatedAt(LocalDateTime.now());
            courseMapper.updateById(course);
        }
        
        // 清除缓存
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
                Teacher teacher = teacherMapper.selectById(dto.getTeacherId());
                if (teacher != null && StringUtils.hasText(teacher.getName())) {
                    // 总是使用最新的教师名称
                    dto.setTeacherName(teacher.getName());
                    // 如果department为空，也设置
                    if (!StringUtils.hasText(dto.getDepartment()) && StringUtils.hasText(teacher.getDepartment())) {
                        dto.setDepartment(teacher.getDepartment());
                    }
                } else {
                    // 如果查询不到教师，但数据库中teacherName有值，保持原值
                    if (!StringUtils.hasText(dto.getTeacherName())) {
                        dto.setTeacherName("未知教师");
                    }
                }
            } catch (Exception e) {
                // 记录异常但继续处理
                System.err.println("获取教师信息失败: teacherId=" + dto.getTeacherId() + ", error=" + e.getMessage());
                // 如果查询失败，但数据库中teacherName有值，保持原值
                if (!StringUtils.hasText(dto.getTeacherName())) {
                    dto.setTeacherName("查询失败");
                }
            }
        } else {
            if (!StringUtils.hasText(dto.getTeacherName())) {
                dto.setTeacherName("未分配");
            }
        }
        
        return dto;
    }
}
