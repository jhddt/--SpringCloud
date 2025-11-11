package com.education.message.feign;

import com.education.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 选课服务Feign客户端
 * 用于验证学生是否选择了某门课程
 */
@FeignClient(name = "selection-service", path = "/selection")
public interface SelectionServiceClient {
    
    /**
     * 检查学生是否选择了某门课程
     */
    @GetMapping("/check")
    Result<Boolean> checkSelection(
            @RequestParam("studentId") Long studentId,
            @RequestParam("courseId") Long courseId
    );
    
    /**
     * 获取课程的所有学生ID列表
     */
    @GetMapping("/course/{courseId}/students")
    Result<?> getCourseStudents(@PathVariable("courseId") Long courseId);
}

