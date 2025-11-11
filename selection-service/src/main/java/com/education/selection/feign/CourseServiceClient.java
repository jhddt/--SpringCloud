package com.education.selection.feign;

import com.education.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 课程服务Feign客户端
 * 用于获取课程信息和更新课程容量
 */
@FeignClient(name = "course-service", path = "/course")
public interface CourseServiceClient {
    
    /**
     * 获取课程信息
     * 使用Map接收，避免直接依赖course-service的DTO
     */
    @GetMapping("/{id}")
    Result<Map<String, Object>> getCourseById(@PathVariable("id") Long id);
    
    /**
     * 获取课程分页列表（用于可选课程列表）
     */
    @GetMapping("/page")
    Result<Map<String, Object>> getCoursePage(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status);
    
    /**
     * 增加课程选课人数
     */
    @PutMapping("/{id}/increment-selected")
    Result<?> incrementSelectedCount(@PathVariable("id") Long id);
    
    /**
     * 减少课程选课人数
     */
    @PutMapping("/{id}/decrement-selected")
    Result<?> decrementSelectedCount(@PathVariable("id") Long id);
}

