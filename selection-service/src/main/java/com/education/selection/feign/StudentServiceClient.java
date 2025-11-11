package com.education.selection.feign;

import com.education.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 学生服务Feign客户端
 * 用于获取学生信息
 */
@FeignClient(name = "student-service", path = "/student")
public interface StudentServiceClient {
    
    /**
     * 获取学生信息
     * 使用Map接收，避免直接依赖student-service的DTO
     */
    @GetMapping("/{studentId}")
    Result<Map<String, Object>> getStudentById(@PathVariable("studentId") Long studentId);
}

