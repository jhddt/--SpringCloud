package com.education.selection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.education.common.exception.BusinessException;
import com.education.selection.dto.SelectionDTO;
import com.education.selection.entity.Enrollment;
import com.education.selection.mapper.EnrollmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SelectionService {
    
    private final EnrollmentMapper enrollmentMapper;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Transactional
    public SelectionDTO selectCourse(Long studentId, Long courseId) {
        // 检查是否已选
        LambdaQueryWrapper<Enrollment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Enrollment::getStudentId, studentId)
               .eq(Enrollment::getCourseId, courseId)
               .eq(Enrollment::getStatus, 0); // 只检查已选状态
        Enrollment exist = enrollmentMapper.selectOne(wrapper);
        if (exist != null) {
            throw new BusinessException(400, "您已选择该课程");
        }
        
        // 使用Redis分布式锁防止并发选课
        String lockKey = "selection:lock:" + courseId;
        String lockValue = String.valueOf(System.currentTimeMillis());
        
        Boolean lockAcquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockValue, java.time.Duration.ofSeconds(10));
        
        if (Boolean.FALSE.equals(lockAcquired)) {
            throw new BusinessException(400, "选课操作过于频繁，请稍后重试");
        }
        
        try {
            // 发送异步消息处理选课
            SelectionDTO dto = new SelectionDTO();
            dto.setStudentId(studentId);
            dto.setCourseId(courseId);
            dto.setStatus(0);
            
            rabbitTemplate.convertAndSend("selection.exchange", "selection.routing", dto);
            
            return dto;
        } finally {
            // 释放锁
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
            redisTemplate.execute(redisScript, Collections.singletonList(lockKey), lockValue);
        }
    }
    
    public void processSelection(SelectionDTO dto) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(dto.getStudentId());
        enrollment.setCourseId(dto.getCourseId());
        enrollment.setStatus(0); // 0-已选
        enrollment.setEnrollmentTime(LocalDateTime.now());
        enrollment.setCreatedAt(LocalDateTime.now());
        enrollment.setUpdatedAt(LocalDateTime.now());
        
        enrollmentMapper.insert(enrollment);
    }
    
    public Page<SelectionDTO> getPage(Integer current, Integer size, Long studentId, Long courseId) {
        Page<Enrollment> page = new Page<>(current, size);
        LambdaQueryWrapper<Enrollment> wrapper = new LambdaQueryWrapper<>();
        
        if (studentId != null) {
            wrapper.eq(Enrollment::getStudentId, studentId);
        }
        if (courseId != null) {
            wrapper.eq(Enrollment::getCourseId, courseId);
        }
        
        wrapper.orderByDesc(Enrollment::getCreatedAt);
        Page<Enrollment> enrollmentPage = enrollmentMapper.selectPage(page, wrapper);
        
        Page<SelectionDTO> dtoPage = new Page<>(current, size, enrollmentPage.getTotal());
        List<SelectionDTO> dtoList = enrollmentPage.getRecords().stream()
                .map(enrollment -> {
                    SelectionDTO result = new SelectionDTO();
                    result.setId(enrollment.getEnrollmentId());
                    result.setStudentId(enrollment.getStudentId());
                    result.setCourseId(enrollment.getCourseId());
                    result.setStatus(enrollment.getStatus());
                    result.setSelectionTime(enrollment.getEnrollmentTime());
                    
                    // TODO: 通过服务间调用获取学生和课程信息
                    // 这里暂时设置为空，后续可以通过 Feign Client 调用 user-service 和 course-service
                    result.setStudentName("未知");
                    result.setCourseName("未知");
                    result.setCourseCode("未知");
                    
                    return result;
                })
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        
        return dtoPage;
    }
    
    @Transactional
    public void cancel(Long id, Long studentId) {
        Enrollment enrollment = enrollmentMapper.selectById(id);
        if (enrollment == null) {
            throw new BusinessException(404, "选课记录不存在");
        }
        
        if (!enrollment.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "无权限取消该选课");
        }
        
        // 更新状态为已退
        enrollment.setStatus(1); // 1-已退
        enrollment.setUpdatedAt(LocalDateTime.now());
        enrollmentMapper.updateById(enrollment);
    }
}
