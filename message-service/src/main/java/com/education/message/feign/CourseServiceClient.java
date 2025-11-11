package com.education.message.feign;

import com.education.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 课程服务Feign客户端
 * 用于验证课程成员关系和获取课程信息
 */
@FeignClient(name = "course-service", path = "/course")
public interface CourseServiceClient {
    
    /**
     * 获取课程信息
     * 使用Map接收，避免直接依赖course-service的DTO
     */
    @GetMapping("/{courseId}")
    Result<Map<String, Object>> getCourseById(@PathVariable("courseId") Long courseId);
}

