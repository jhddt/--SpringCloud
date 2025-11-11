package com.education.message.feign;

import com.education.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 教师服务Feign客户端
 * 用于获取教师信息和验证教师身份
 */
@FeignClient(name = "teacher-service", path = "/teacher")
public interface TeacherServiceClient {
    
    /**
     * 获取教师信息
     */
    @GetMapping("/{teacherId}")
    Result<?> getTeacherById(@PathVariable("teacherId") Long teacherId);
    
    /**
     * 通过userId获取教师信息
     */
    @GetMapping("/user/{userId}")
    Result<?> getTeacherByUserId(@PathVariable("userId") Long userId);
}

