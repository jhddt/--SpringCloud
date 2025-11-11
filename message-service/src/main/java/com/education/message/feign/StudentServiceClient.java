package com.education.message.feign;

import com.education.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 学生服务Feign客户端
 * 用于获取学生信息和验证学生身份
 */
@FeignClient(name = "student-service", path = "/student")
public interface StudentServiceClient {
    
    /**
     * 获取学生信息
     */
    @GetMapping("/{studentId}")
    Result<?> getStudentById(@PathVariable("studentId") Long studentId);
    
    /**
     * 通过userId获取学生信息
     */
    @GetMapping("/user/{userId}")
    Result<?> getStudentByUserId(@PathVariable("userId") Long userId);
}

