package com.education.selection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.education.common.constant.Constants;
import com.education.common.exception.BusinessException;
import com.education.common.result.Result;
import com.education.selection.dto.SelectionDTO;
import com.education.selection.entity.Enrollment;
import com.education.selection.feign.CourseServiceClient;
import com.education.selection.feign.MessageServiceClient;
import com.education.selection.feign.StudentServiceClient;
import feign.FeignException;
import com.education.selection.mapper.EnrollmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 选课服务
 * 按照流程图实现完整的选课流程：
 * 1. 登录验证 - 用户身份 + 角色校验（由网关处理）
 * 2. 拉取可选课程 - 过滤专业/容量/时间
 * 3. 提交选课请求 - 验证冲突/学分/重复选
 * 4. 并发与锁控制 - Redis原子操作
 * 5. 写入数据库 - enrollments、更新course容量
 * 6. 消息推送通知 - 学生、教师、教务同步
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SelectionService {
    
    private final EnrollmentMapper enrollmentMapper;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StudentServiceClient studentServiceClient;
    private final CourseServiceClient courseServiceClient;
    private final MessageServiceClient messageServiceClient;
    
    // 最大选课学分限制
    private static final BigDecimal MAX_TOTAL_CREDITS = new BigDecimal("30");
    
    /**
     * 获取可选课程列表（过滤专业/容量/时间）
     * 步骤2：拉取可选课程
     */
    public Page<Map<String, Object>> getAvailableCourses(Long studentId, Integer current, Integer size, String keyword) {
        log.info("获取可选课程列表: studentId={}, current={}, size={}, keyword={}", studentId, current, size, keyword);
        
        // 1. 获取学生信息（如果studentId为空，跳过学生相关检查）
        // 使用临时变量构建数据，最后赋值给final变量以确保可以在lambda表达式中使用
        String tempStudentMajor = null;
        Set<Long> tempSelectedCourseIds = new HashSet<>();
        Map<Long, Map<String, Object>> tempSelectedCoursesTimeInfo = new HashMap<>();
        
        if (studentId != null) {
            try {
                Map<String, Object> studentInfo = getStudentInfo(studentId);
                tempStudentMajor = (String) studentInfo.get("major");
                
                // 3. 获取学生已选课程
                List<Enrollment> studentEnrollments = enrollmentMapper.selectList(
                    new LambdaQueryWrapper<Enrollment>()
                        .eq(Enrollment::getStudentId, studentId)
                        .eq(Enrollment::getStatus, 0) // 0-已选
                );
                tempSelectedCourseIds = studentEnrollments.stream()
                    .map(Enrollment::getCourseId)
                    .collect(Collectors.toSet());
                
                // 4. 获取学生已选课程的时间信息（用于时间冲突检查）
                for (Enrollment enrollment : studentEnrollments) {
                    try {
                        Result<Map<String, Object>> courseResult = courseServiceClient.getCourseById(enrollment.getCourseId());
                        if (courseResult != null && courseResult.getCode() == 200 && courseResult.getData() != null) {
                            tempSelectedCoursesTimeInfo.put(enrollment.getCourseId(), courseResult.getData());
                        }
                    } catch (Exception e) {
                        log.warn("获取已选课程信息失败, courseId: {}", enrollment.getCourseId(), e);
                    }
                }
            } catch (BusinessException e) {
                log.warn("获取学生信息失败，studentId: {}, 错误: {}", studentId, e.getMessage());
                // 学生信息不存在时，仍然可以查看课程，只是不能选课
                // 使用默认的空值，已在声明时初始化
            }
        }
        
        // 将临时变量赋值给final变量，用于lambda表达式
        final String studentMajor = tempStudentMajor;
        final Set<Long> selectedCourseIds = tempSelectedCourseIds;
        final Map<Long, Map<String, Object>> selectedCoursesTimeInfo = tempSelectedCoursesTimeInfo;
        
        // 2. 获取所有开放选课的课程（分批获取，确保获取所有课程）
        List<Map<String, Object>> allCourses = new ArrayList<>();
        int pageSize = 100; // 每批获取100条
        int pageNum = 1;
        boolean hasMore = true;
        
        while (hasMore) {
            Result<Map<String, Object>> coursePageResult = courseServiceClient.getCoursePage(
                pageNum, pageSize, keyword, 1);
            
            if (coursePageResult == null || coursePageResult.getCode() != 200 || coursePageResult.getData() == null) {
                log.error("获取课程列表失败: pageNum={}, pageSize={}", pageNum, pageSize);
                break;
            }
            
            Map<String, Object> coursePageData = coursePageResult.getData();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> courses = (List<Map<String, Object>>) coursePageData.get("records");
            
            if (courses == null || courses.isEmpty()) {
                hasMore = false;
            } else {
                allCourses.addAll(courses);
                // 如果返回的课程数量小于请求的数量，说明已经获取完所有课程
                if (courses.size() < pageSize) {
                    hasMore = false;
                } else {
                    pageNum++;
                    // 为了防止无限循环，设置一个最大页数限制（比如100页，即最多10000条课程）
                    if (pageNum > 100) {
                        log.warn("课程数量过多，已获取前{}条课程", allCourses.size());
                        hasMore = false;
                    }
                }
            }
        }
        
        log.info("获取到{}条开放选课的课程", allCourses.size());
        
        // 5. 过滤课程：专业匹配、容量、时间冲突、重复选
        List<Map<String, Object>> availableCourses = allCourses.stream()
            .filter(course -> {
                Object courseIdObj = course.get("courseId");
                if (courseIdObj == null) {
                    courseIdObj = course.get("id"); // 兼容不同的字段名
                }
                if (courseIdObj == null) {
                    return false;
                }
                Long courseId = ((Number) courseIdObj).longValue();
                
                // 过滤1：排除已选课程（如果studentId不为空）
                if (studentId != null && selectedCourseIds.contains(courseId)) {
                    return false;
                }
                
                // 过滤2：专业匹配（如果课程有department字段，需要匹配学生的major）
                // 注意：如果学生信息不存在，跳过专业匹配检查
                String courseDepartment = (String) course.get("department");
                if (StringUtils.hasText(courseDepartment) && StringUtils.hasText(studentMajor)) {
                    // 这里可以根据实际需求调整专业匹配逻辑
                    // 如果课程department为空，则不限制专业
                    // 暂时不限制专业匹配，允许所有专业的学生选课
                }
                
                // 过滤3：容量检查
                Object totalCapacityObj = course.get("totalCapacity");
                Object selectedCountObj = course.get("selectedCount");
                Integer totalCapacity = totalCapacityObj != null ? ((Number) totalCapacityObj).intValue() : null;
                Integer selectedCount = selectedCountObj != null ? ((Number) selectedCountObj).intValue() : null;
                if (totalCapacity != null && selectedCount != null && selectedCount >= totalCapacity) {
                    return false;
                }
                
                // 过滤4：时间冲突检查（简化版：如果课程有startTime和endTime，检查是否与已选课程时间重叠）
                // 只在studentId不为空时进行时间冲突检查
                if (studentId != null) {
                    LocalDateTime courseStartTime = parseDateTime(course.get("startTime"));
                    LocalDateTime courseEndTime = parseDateTime(course.get("endTime"));
                    if (courseStartTime != null && courseEndTime != null) {
                        for (Map<String, Object> selectedCourse : selectedCoursesTimeInfo.values()) {
                            LocalDateTime selectedStartTime = parseDateTime(selectedCourse.get("startTime"));
                            LocalDateTime selectedEndTime = parseDateTime(selectedCourse.get("endTime"));
                            if (selectedStartTime != null && selectedEndTime != null) {
                                // 检查时间是否重叠
                                if (isTimeOverlap(courseStartTime, courseEndTime, selectedStartTime, selectedEndTime)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
                
                return true;
            })
            .collect(Collectors.toList());
        
        // 6. 手动分页（基于过滤后的结果）
        long total = availableCourses.size();
        int start = (current - 1) * size;
        int end = Math.min(start + size, availableCourses.size());
        
        List<Map<String, Object>> pagedCourses = new ArrayList<>();
        if (start < availableCourses.size()) {
            pagedCourses = availableCourses.subList(start, end);
        }
        
        // 7. 构建分页结果
        Page<Map<String, Object>> page = new Page<>(current, size, total);
        page.setRecords(pagedCourses);
        
        log.info("可选课程列表: 总数={}, 当前页={}, 每页数量={}, 返回数量={}", 
            total, current, size, pagedCourses.size());
        
        return page;
    }
    
    /**
     * 选课
     * 步骤3-6：提交选课请求 -> 并发控制 -> 写入数据库 -> 消息通知
     */
    @Transactional
    public SelectionDTO selectCourse(Long studentId, Long courseId) {
        log.info("开始选课流程: studentId={}, courseId={}", studentId, courseId);
        
        try {
            // 步骤3：提交选课请求 - 验证冲突/学分/重复选
            
            // 3.1 验证1：检查是否已选（检查所有状态的记录，因为数据库有唯一约束）
            LambdaQueryWrapper<Enrollment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Enrollment::getStudentId, studentId)
                   .eq(Enrollment::getCourseId, courseId);
            Enrollment exist = enrollmentMapper.selectOne(wrapper);
            if (exist != null) {
                // 如果记录存在且状态为0（已选），则提示已选
                if (exist.getStatus() != null && exist.getStatus() == 0) {
                    log.warn("选课失败：学生已选择该课程, studentId={}, courseId={}", studentId, courseId);
                    throw new BusinessException(400, "您已选择该课程");
                }
                // 如果记录存在但状态不为0（比如已退课），允许重新选课，后续会更新状态
                log.info("发现已存在的选课记录（可能已退课），将更新为已选状态: studentId={}, courseId={}, currentStatus={}", 
                    studentId, courseId, exist.getStatus());
            }
            
            // 3.2 获取学生信息
            log.debug("获取学生信息: studentId={}", studentId);
            Map<String, Object> studentInfo;
            try {
                studentInfo = getStudentInfo(studentId);
                log.debug("学生信息获取成功: studentId={}, studentInfo={}", studentId, studentInfo);
            } catch (Exception e) {
                log.error("获取学生信息失败: studentId={}", studentId, e);
                throw new BusinessException(404, "学生信息不存在");
            }
            String studentMajor = (String) studentInfo.get("major");
            
            // 3.3 获取课程信息
            log.debug("获取课程信息: courseId={}", courseId);
            Map<String, Object> courseInfo;
            try {
                courseInfo = getCourseInfo(courseId);
                log.debug("课程信息获取成功: courseId={}, courseInfo={}", courseId, courseInfo);
            } catch (Exception e) {
                log.error("获取课程信息失败: courseId={}", courseId, e);
                throw new BusinessException(404, "课程信息不存在");
            }
            
            // 3.4 验证2：专业匹配
            String courseDepartment = (String) courseInfo.get("department");
            if (StringUtils.hasText(courseDepartment) && StringUtils.hasText(studentMajor)) {
                // 这里可以根据实际需求调整专业匹配逻辑
                // 如果课程department为空，则不限制专业
            }
            
            // 3.5 验证3：容量检查
            // 注意：如果记录已存在且状态不为0（退课），容量检查时需要考虑：
            // - 学生退课时，容量已经减少
            // - 重新选课时，如果容量已满，说明有其他学生选课了，这是正常的
            // - 但如果容量已满，仍然不允许重新选课（除非有特殊业务需求）
            Integer totalCapacity = parseInteger(courseInfo.get("totalCapacity"));
            Integer selectedCount = parseInteger(courseInfo.get("selectedCount"));
            log.debug("课程容量检查: courseId={}, totalCapacity={}, selectedCount={}, existRecord={}", 
                courseId, totalCapacity, selectedCount, exist != null);
            
            // 如果记录已存在且状态不为0（退课），容量检查应该可以通过
            // 因为学生退课时容量已经减少了，重新选课只是恢复
            // 但如果容量已满，说明有其他学生选课了，不允许重新选课
            if (exist == null || exist.getStatus() == null || exist.getStatus() == 0) {
                // 新选课或已选状态：正常检查容量
                if (totalCapacity != null && selectedCount != null && selectedCount >= totalCapacity) {
                    log.warn("选课失败：课程容量已满, courseId={}, totalCapacity={}, selectedCount={}", 
                        courseId, totalCapacity, selectedCount);
                    throw new BusinessException(400, "课程容量已满");
                }
            } else {
                // 重新选课（之前退课）：如果容量已满，不允许重新选课
                // 因为容量被其他学生占用了
                if (totalCapacity != null && selectedCount != null && selectedCount >= totalCapacity) {
                    log.warn("重新选课失败：课程容量已满, courseId={}, totalCapacity={}, selectedCount={}", 
                        courseId, totalCapacity, selectedCount);
                    throw new BusinessException(400, "课程容量已满，无法重新选课");
                }
            }
            
            // 3.6 验证4：时间冲突检查
            LocalDateTime courseStartTime = parseDateTime(courseInfo.get("startTime"));
            LocalDateTime courseEndTime = parseDateTime(courseInfo.get("endTime"));
            if (courseStartTime != null && courseEndTime != null) {
                log.debug("检查时间冲突: courseId={}, startTime={}, endTime={}", courseId, courseStartTime, courseEndTime);
                List<Enrollment> studentEnrollments = enrollmentMapper.selectList(
                    new LambdaQueryWrapper<Enrollment>()
                        .eq(Enrollment::getStudentId, studentId)
                        .eq(Enrollment::getStatus, 0) // 0-已选
                );
                
                for (Enrollment enrollment : studentEnrollments) {
                    try {
                        Map<String, Object> selectedCourseInfo = getCourseInfo(enrollment.getCourseId());
                        LocalDateTime selectedStartTime = parseDateTime(selectedCourseInfo.get("startTime"));
                        LocalDateTime selectedEndTime = parseDateTime(selectedCourseInfo.get("endTime"));
                        
                        if (selectedStartTime != null && selectedEndTime != null) {
                            if (isTimeOverlap(courseStartTime, courseEndTime, selectedStartTime, selectedEndTime)) {
                                log.warn("选课失败：时间冲突, studentId={}, courseId={}, selectedCourseId={}", 
                                    studentId, courseId, enrollment.getCourseId());
                                throw new BusinessException(400, "该课程与已选课程时间冲突");
                            }
                        }
                    } catch (BusinessException e) {
                        throw e;
                    } catch (Exception e) {
                        log.warn("检查已选课程时间冲突时出错: enrollmentId={}", enrollment.getEnrollmentId(), e);
                        // 继续检查其他课程
                    }
                }
            }
            
            // 3.7 验证5：学分限制检查
            BigDecimal courseCredit = parseBigDecimal(courseInfo.get("credit"));
            if (courseCredit != null) {
                BigDecimal totalCredits = calculateTotalCredits(studentId);
                log.debug("学分检查: studentId={}, courseCredit={}, totalCredits={}, maxCredits={}", 
                    studentId, courseCredit, totalCredits, MAX_TOTAL_CREDITS);
                if (totalCredits.add(courseCredit).compareTo(MAX_TOTAL_CREDITS) > 0) {
                    log.warn("选课失败：学分超限, studentId={}, courseCredit={}, totalCredits={}, maxCredits={}", 
                        studentId, courseCredit, totalCredits, MAX_TOTAL_CREDITS);
                    throw new BusinessException(400, String.format("选课总学分不能超过%.0f学分", MAX_TOTAL_CREDITS));
                }
            }
            
            // 步骤4：并发与锁控制 - Redis原子操作
            String lockKey = "selection:lock:" + studentId + ":" + courseId;
            String lockValue = UUID.randomUUID().toString();
            
            // 使用SET NX EX实现分布式锁
            Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, java.time.Duration.ofSeconds(10));
            
            if (Boolean.FALSE.equals(lockAcquired)) {
                log.warn("选课失败：获取锁失败, studentId={}, courseId={}", studentId, courseId);
                throw new BusinessException(400, "选课操作过于频繁，请稍后重试");
            }
            
            try {
                // 双重检查：在锁内再次检查容量和重复选
                exist = enrollmentMapper.selectOne(wrapper);
                if (exist != null && exist.getStatus() != null && exist.getStatus() == 0) {
                    log.warn("选课失败：锁内检查发现已选, studentId={}, courseId={}", studentId, courseId);
                    throw new BusinessException(400, "您已选择该课程");
                }
                
                // 重新获取课程信息，确保容量是最新的
                courseInfo = getCourseInfo(courseId);
                selectedCount = parseInteger(courseInfo.get("selectedCount"));
                totalCapacity = parseInteger(courseInfo.get("totalCapacity"));
                log.debug("锁内容量检查: courseId={}, totalCapacity={}, selectedCount={}, existRecord={}", 
                    courseId, totalCapacity, selectedCount, exist != null);
                
                // 锁内容量检查：同样需要考虑重新选课的情况
                if (exist == null || exist.getStatus() == null || exist.getStatus() == 0) {
                    // 新选课或已选状态：正常检查容量
                    if (totalCapacity != null && selectedCount != null && selectedCount >= totalCapacity) {
                        log.warn("选课失败：锁内检查发现容量已满, courseId={}, totalCapacity={}, selectedCount={}", 
                            courseId, totalCapacity, selectedCount);
                        throw new BusinessException(400, "课程容量已满");
                    }
                } else {
                    // 重新选课（之前退课）：如果容量已满，不允许重新选课
                    if (totalCapacity != null && selectedCount != null && selectedCount >= totalCapacity) {
                        log.warn("重新选课失败：锁内检查发现容量已满, courseId={}, totalCapacity={}, selectedCount={}", 
                            courseId, totalCapacity, selectedCount);
                        throw new BusinessException(400, "课程容量已满，无法重新选课");
                    }
                }
                
                // 步骤5：写入数据库
                Enrollment enrollment;
                Integer oldStatus = null;
                boolean isReSelection = false; // 是否是重新选课（从退课状态恢复）
                
                if (exist != null) {
                    // 如果记录已存在（可能是已退课状态），则更新状态为已选
                    oldStatus = exist.getStatus(); // 保存原始状态
                    isReSelection = (oldStatus != null && oldStatus != 0); // 如果是退课状态，则是重新选课
                    
                    enrollment = exist;
                    enrollment.setStatus(0); // 0-已选
                    enrollment.setEnrollmentTime(LocalDateTime.now()); // 更新选课时间
                    enrollment.setUpdatedAt(LocalDateTime.now());
                    
                    log.debug("更新选课记录状态: enrollmentId={}, studentId={}, courseId={}, oldStatus={}", 
                        enrollment.getEnrollmentId(), studentId, courseId, oldStatus);
                    enrollmentMapper.updateById(enrollment);
                    log.info("选课记录已更新: enrollmentId={}, studentId={}, courseId={}, oldStatus={}, newStatus=0", 
                        enrollment.getEnrollmentId(), studentId, courseId, oldStatus);
                } else {
                    // 如果记录不存在，则插入新记录
                    enrollment = new Enrollment();
                    enrollment.setStudentId(studentId);
                    enrollment.setCourseId(courseId);
                    enrollment.setStatus(0); // 0-已选
                    enrollment.setEnrollmentTime(LocalDateTime.now());
                    enrollment.setCreatedAt(LocalDateTime.now());
                    enrollment.setUpdatedAt(LocalDateTime.now());
                    
                    log.debug("插入选课记录: studentId={}, courseId={}", studentId, courseId);
                    enrollmentMapper.insert(enrollment);
                    log.info("选课记录已插入: enrollmentId={}, studentId={}, courseId={}", 
                        enrollment.getEnrollmentId(), studentId, courseId);
                }
                
                // 更新课程选课人数
                // 注意：如果是重新选课（从退课状态恢复），需要增加课程容量
                // 如果是新选课，也需要增加课程容量
                // 但如果记录已存在且状态已经是0（已选），则不应该增加容量（这种情况理论上不应该发生）
                // 但是，如果记录已存在且状态已经是0，说明之前已经选过课了，不应该再次增加容量
                // 这种情况应该在前面就被拦截了，但为了安全起见，这里也做判断
                if (oldStatus == null || oldStatus != 0) {
                    // 只有在新选课或重新选课（从退课状态恢复）时才增加容量
                    try {
                        log.debug("更新课程选课人数: courseId={}, isReSelection={}, oldStatus={}", courseId, isReSelection, oldStatus);
                        Result<?> updateResult = courseServiceClient.incrementSelectedCount(courseId);
                        if (updateResult == null || updateResult.getCode() != 200) {
                            log.error("更新课程选课人数失败: courseId={}, result={}", courseId, updateResult);
                            // 如果是404错误，可能是接口路径问题或服务未启动
                            if (updateResult == null) {
                                throw new BusinessException(500, "更新课程容量失败：无法连接到课程服务");
                            }
                            throw new BusinessException(500, "更新课程容量失败: " + (updateResult.getMessage() != null ? updateResult.getMessage() : "未知错误"));
                        }
                        log.info("课程选课人数已更新: courseId={}, isReSelection={}", courseId, isReSelection);
                    } catch (FeignException e) {
                        log.error("更新课程选课人数失败（Feign异常）: courseId={}, status={}, message={}", 
                            courseId, e.status(), e.getMessage(), e);
                        // 如果更新失败，回滚选课记录（事务会自动回滚）
                        if (e.status() == 404) {
                            // 404可能是接口不存在，也可能是课程不存在
                            String errorMsg = "更新课程容量失败：";
                            if (e.contentUTF8() != null && e.contentUTF8().contains("课程不存在")) {
                                errorMsg += "课程不存在（courseId=" + courseId + "）";
                            } else {
                                errorMsg += "课程服务接口不存在或课程不存在，请检查课程服务是否正常运行";
                            }
                            throw new BusinessException(500, errorMsg);
                        }
                        throw new BusinessException(500, "更新课程容量失败: " + e.getMessage());
                    } catch (Exception e) {
                        log.error("更新课程选课人数失败: courseId={}", courseId, e);
                        // 如果更新失败，回滚选课记录（事务会自动回滚）
                        throw new BusinessException(500, "更新课程容量失败: " + e.getMessage());
                    }
                } else {
                    log.warn("记录已存在且状态为0（已选），跳过增加容量: courseId={}, enrollmentId={}", 
                        courseId, enrollment.getEnrollmentId());
                }
                
                // 步骤6：消息推送通知
                sendSelectionNotification(studentId, courseId, studentInfo, courseInfo);
                
                // 构建返回结果
                SelectionDTO result = new SelectionDTO();
                result.setId(enrollment.getEnrollmentId());
                result.setStudentId(studentId);
                result.setCourseId(courseId);
                result.setStatus(0);
                result.setSelectionTime(enrollment.getEnrollmentTime());
                result.setStudentName((String) studentInfo.get("name"));
                result.setCourseName((String) courseInfo.get("courseName"));
                result.setCourseCode((String) courseInfo.get("courseCode"));
                result.setTeacherName((String) courseInfo.get("teacherName"));
                result.setCredit(courseCredit);
                // 设置课程封面
                Object coverImage = courseInfo.get("coverImage");
                if (coverImage != null) {
                    result.setCoverImage(coverImage.toString());
                }
                
                log.info("选课成功: studentId={}, courseId={}, enrollmentId={}", studentId, courseId, enrollment.getEnrollmentId());
                return result;
                
            } finally {
                // 释放锁
                try {
                    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
                    redisTemplate.execute(redisScript, Collections.singletonList(lockKey), lockValue);
                    log.debug("释放选课锁: lockKey={}", lockKey);
                } catch (Exception e) {
                    log.error("释放选课锁失败: lockKey={}", lockKey, e);
                }
            }
        } catch (BusinessException e) {
            log.error("选课业务异常: studentId={}, courseId={}, error={}", studentId, courseId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("选课系统异常: studentId={}, courseId={}", studentId, courseId, e);
            throw new BusinessException(500, "选课失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理选课（异步消息处理，保留兼容性）
     */
    public void processSelection(SelectionDTO dto) {
        log.info("处理选课消息: studentId={}, courseId={}", dto.getStudentId(), dto.getCourseId());
        // 注意：这个方法现在已经不需要了，因为选课已经同步处理
        // 但为了兼容性，保留这个方法
        try {
            selectCourse(dto.getStudentId(), dto.getCourseId());
        } catch (Exception e) {
            log.error("处理选课消息失败", e);
            throw e;
        }
    }
    
    /**
     * 获取选课分页列表
     */
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
                    
                    // 通过 Feign Client 获取学生信息
                    try {
                        Result<Map<String, Object>> studentResult = studentServiceClient.getStudentById(enrollment.getStudentId());
                        if (studentResult != null && studentResult.getCode() == 200 && studentResult.getData() != null) {
                            Map<String, Object> studentData = studentResult.getData();
                            // 获取学生姓名
                            Object name = studentData.get("name");
                            if (name != null && !name.toString().trim().isEmpty()) {
                                result.setStudentName(name.toString());
                            } else {
                                result.setStudentName("未知");
                                log.warn("学生姓名为空, studentId: {}", enrollment.getStudentId());
                            }
                            // 获取学号（username）
                            Object username = studentData.get("username");
                            if (username != null && !username.toString().trim().isEmpty()) {
                                result.setStudentNo(username.toString());
                            } else {
                                result.setStudentNo("未知");
                                log.warn("学生学号为空, studentId: {}", enrollment.getStudentId());
                            }
                            log.debug("获取学生信息成功: studentId={}, name={}, username={}", 
                                enrollment.getStudentId(), result.getStudentName(), result.getStudentNo());
                        } else {
                            log.warn("获取学生信息失败: studentId={}, result={}", enrollment.getStudentId(), studentResult);
                            result.setStudentName("未知");
                            result.setStudentNo("未知");
                        }
                    } catch (Exception e) {
                        log.error("获取学生信息异常: studentId={}", enrollment.getStudentId(), e);
                        result.setStudentName("未知");
                        result.setStudentNo("未知");
                    }
                    
                    // 通过 Feign Client 获取课程信息
                    try {
                        Result<Map<String, Object>> courseResult = courseServiceClient.getCourseById(enrollment.getCourseId());
                        if (courseResult != null && courseResult.getCode() == 200 && courseResult.getData() != null) {
                            Map<String, Object> courseData = courseResult.getData();
                            Object courseName = courseData.get("courseName");
                            Object courseCode = courseData.get("courseCode");
                            Object teacherName = courseData.get("teacherName");
                            Object credit = courseData.get("credit");
                            Object coverImage = courseData.get("coverImage");
                            
                            if (courseName != null) {
                                result.setCourseName(courseName.toString());
                            } else {
                                result.setCourseName("未知");
                            }
                            if (courseCode != null) {
                                result.setCourseCode(courseCode.toString());
                            } else {
                                result.setCourseCode("未知");
                            }
                            if (teacherName != null) {
                                result.setTeacherName(teacherName.toString());
                            }
                            if (credit != null) {
                                try {
                                    if (credit instanceof Number) {
                                        result.setCredit(BigDecimal.valueOf(((Number) credit).doubleValue()));
                                    } else {
                                        result.setCredit(new BigDecimal(credit.toString()));
                                    }
                                } catch (Exception e) {
                                    log.warn("解析学分失败, credit: {}", credit, e);
                                }
                            }
                            // 设置课程封面
                            if (coverImage != null) {
                                result.setCoverImage(coverImage.toString());
                            }
                        } else {
                            result.setCourseName("未知");
                            result.setCourseCode("未知");
                        }
                    } catch (Exception e) {
                        log.warn("获取课程信息失败, courseId: {}", enrollment.getCourseId(), e);
                        result.setCourseName("未知");
                        result.setCourseCode("未知");
                    }
                    
                    // 设置成绩
                    if (enrollment.getScore() != null) {
                        result.setScore(BigDecimal.valueOf(enrollment.getScore()));
                    }
                    
                    return result;
                })
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        
        return dtoPage;
    }
    
    /**
     * 取消选课
     */
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
        
        // 减少课程选课人数
        try {
            courseServiceClient.decrementSelectedCount(enrollment.getCourseId());
            log.info("课程选课人数已减少: courseId={}", enrollment.getCourseId());
        } catch (Exception e) {
            log.error("减少课程选课人数失败: courseId={}", enrollment.getCourseId(), e);
        }
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 获取学生信息
     */
    private Map<String, Object> getStudentInfo(Long studentId) {
        Result<Map<String, Object>> result = studentServiceClient.getStudentById(studentId);
        if (result == null || result.getCode() != 200 || result.getData() == null) {
            throw new BusinessException(404, "学生信息不存在");
        }
        return result.getData();
    }
    
    /**
     * 获取课程信息
     */
    private Map<String, Object> getCourseInfo(Long courseId) {
        Result<Map<String, Object>> result = courseServiceClient.getCourseById(courseId);
        if (result == null || result.getCode() != 200 || result.getData() == null) {
            throw new BusinessException(404, "课程信息不存在");
        }
        return result.getData();
    }
    
    /**
     * 计算学生总学分
     */
    private BigDecimal calculateTotalCredits(Long studentId) {
        List<Enrollment> enrollments = enrollmentMapper.selectList(
            new LambdaQueryWrapper<Enrollment>()
                .eq(Enrollment::getStudentId, studentId)
                .eq(Enrollment::getStatus, 0) // 0-已选
        );
        
        BigDecimal totalCredits = BigDecimal.ZERO;
        for (Enrollment enrollment : enrollments) {
            try {
                Map<String, Object> courseInfo = getCourseInfo(enrollment.getCourseId());
                BigDecimal credit = parseBigDecimal(courseInfo.get("credit"));
                if (credit != null) {
                    totalCredits = totalCredits.add(credit);
                }
            } catch (Exception e) {
                log.warn("获取课程学分失败, courseId: {}", enrollment.getCourseId(), e);
            }
        }
        
        return totalCredits;
    }
    
    /**
     * 检查时间是否重叠
     */
    private boolean isTimeOverlap(LocalDateTime start1, LocalDateTime end1, 
                                  LocalDateTime start2, LocalDateTime end2) {
        // 时间重叠的条件：start1 < end2 && start2 < end1
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
    
    /**
     * 解析LocalDateTime
     */
    private LocalDateTime parseDateTime(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof LocalDateTime) {
            return (LocalDateTime) obj;
        }
        if (obj instanceof String) {
            try {
                return LocalDateTime.parse((String) obj);
            } catch (Exception e) {
                log.warn("解析时间失败: {}", obj, e);
                return null;
            }
        }
        return null;
    }
    
    /**
     * 解析BigDecimal
     */
    private BigDecimal parseBigDecimal(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof BigDecimal) {
            return (BigDecimal) obj;
        }
        if (obj instanceof Number) {
            return BigDecimal.valueOf(((Number) obj).doubleValue());
        }
        try {
            return new BigDecimal(obj.toString());
        } catch (Exception e) {
            log.warn("解析BigDecimal失败: {}", obj, e);
            return null;
        }
    }
    
    /**
     * 解析Integer
     * 安全地从Map中获取Integer值，处理不同的Number类型
     */
    private Integer parseInteger(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        try {
            return Integer.parseInt(obj.toString());
        } catch (Exception e) {
            log.warn("解析Integer失败: {}", obj, e);
            return null;
        }
    }
    
    /**
     * 发送选课通知
     * 注意：由于消息服务接口限制，这里暂时使用RabbitMQ发送异步通知
     * 或者可以通过网关转发请求，在请求头中设置系统用户信息
     */
    private void sendSelectionNotification(Long studentId, Long courseId, 
                                          Map<String, Object> studentInfo, 
                                          Map<String, Object> courseInfo) {
        try {
            String studentName = (String) studentInfo.get("name");
            String courseName = (String) courseInfo.get("courseName");
            Long teacherId = parseLong(courseInfo.get("teacherId"));
            
            // 由于消息服务需要从请求头获取用户信息，而Feign客户端无法直接设置请求头
            // 这里我们使用RabbitMQ发送异步通知，或者记录日志
            // 实际项目中可以通过网关转发，在网关中设置系统用户请求头
            
            log.info("选课通知 - 学生: {}, 课程: {}, 教师: {}", studentName, courseName, teacherId);
            
            // 可以通过RabbitMQ发送通知消息到消息服务
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "SELECTION_SUCCESS");
            notification.put("studentId", studentId);
            notification.put("studentName", studentName);
            notification.put("courseId", courseId);
            notification.put("courseName", courseName);
            notification.put("teacherId", teacherId);
            notification.put("timestamp", System.currentTimeMillis());
            
            try {
                rabbitTemplate.convertAndSend(Constants.EXCHANGE_SELECTION, "selection.notification", notification);
                log.info("选课通知消息已发送到队列: studentId={}, courseId={}", studentId, courseId);
            } catch (Exception e) {
                log.warn("发送选课通知消息到队列失败", e);
            }
            
        } catch (Exception e) {
            log.error("发送选课通知失败", e);
            // 通知失败不影响选课流程
        }
    }
    
    /**
     * 解析Long
     */
    private Long parseLong(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Long) {
            return (Long) obj;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            return Long.parseLong(obj.toString());
        } catch (Exception e) {
            log.warn("解析Long失败: {}", obj, e);
            return null;
        }
    }
    
    /**
     * 更新成绩
     */
    @Transactional
    public void updateScore(Long enrollmentId, Double score) {
        log.info("更新成绩: enrollmentId={}, score={}", enrollmentId, score);
        
        // 验证成绩范围
        if (score != null && (score < 0 || score > 100)) {
            throw new BusinessException(400, "成绩必须在0-100之间");
        }
        
        // 查询选课记录
        Enrollment enrollment = enrollmentMapper.selectById(enrollmentId);
        if (enrollment == null) {
            throw new BusinessException(404, "选课记录不存在");
        }
        
        // 只能为已通过的选课记录录入成绩
        if (enrollment.getStatus() == null || enrollment.getStatus() != 0) {
            throw new BusinessException(400, "只能为已选课程录入成绩");
        }
        
        // 更新成绩
        enrollment.setScore(score);
        enrollment.setUpdatedAt(LocalDateTime.now());
        enrollmentMapper.updateById(enrollment);
        
        log.info("成绩更新成功: enrollmentId={}, score={}", enrollmentId, score);
    }
    
    /**
     * 批量更新成绩
     */
    @Transactional
    public void batchUpdateScore(List<Map<String, Object>> scoreList) {
        log.info("批量更新成绩: count={}", scoreList.size());
        
        for (Map<String, Object> item : scoreList) {
            Long enrollmentId = parseLong(item.get("enrollmentId"));
            Object scoreObj = item.get("score");
            Double score = null;
            
            if (scoreObj != null) {
                if (scoreObj instanceof Number) {
                    score = ((Number) scoreObj).doubleValue();
                } else {
                    try {
                        score = Double.parseDouble(scoreObj.toString());
                    } catch (Exception e) {
                        log.warn("解析成绩失败: enrollmentId={}, score={}", enrollmentId, scoreObj);
                        continue;
                    }
                }
            }
            
            if (enrollmentId != null) {
                try {
                    updateScore(enrollmentId, score);
                } catch (Exception e) {
                    log.error("更新成绩失败: enrollmentId={}, score={}", enrollmentId, score, e);
                    // 继续处理其他记录
                }
            }
        }
        
        log.info("批量更新成绩完成");
    }
}
