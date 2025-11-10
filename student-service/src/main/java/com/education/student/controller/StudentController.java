package com.education.student.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.education.common.constant.Constants;
import com.education.common.exception.BusinessException;
import com.education.common.result.Result;
import com.education.student.dto.StudentDTO;
import com.education.student.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {
    
    private final StudentService studentService;
    
    @GetMapping("/{id}")
    public Result<StudentDTO> getById(@PathVariable("id") Long id) {
        StudentDTO dto = studentService.getById(id);
        return Result.success(dto);
    }
    
    @GetMapping("/page")
    public Result<Page<StudentDTO>> getPage(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword) {
        Page<StudentDTO> page = studentService.getPage(current, size, keyword);
        return Result.success(page);
    }
    
    @PostMapping
    public Result<StudentDTO> create(
            @Valid @RequestBody StudentDTO dto,
            @RequestHeader(value = "X-Role", required = false) String role) {
        // 权限验证：只有管理员可以创建学生
        if (!StringUtils.hasText(role) || !Constants.ROLE_ADMIN.equals(role)) {
            throw new BusinessException(403, "只有管理员可以创建学生账号");
        }
        StudentDTO result = studentService.create(dto);
        return Result.success("创建成功", result);
    }
    
    @PutMapping("/{id}")
    public Result<StudentDTO> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody StudentDTO dto,
            @RequestHeader(value = "X-Role", required = false) String role) {
        // 权限验证：只有管理员可以修改学生信息
        if (!StringUtils.hasText(role) || !Constants.ROLE_ADMIN.equals(role)) {
            throw new BusinessException(403, "只有管理员可以修改学生信息");
        }
        StudentDTO result = studentService.update(id, dto);
        return Result.success("更新成功", result);
    }
    
    @DeleteMapping("/{id}")
    public Result<?> delete(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Role", required = false) String role) {
        // 权限验证：只有管理员可以删除学生
        if (!StringUtils.hasText(role) || !Constants.ROLE_ADMIN.equals(role)) {
            throw new BusinessException(403, "只有管理员可以删除学生账号");
        }
        studentService.delete(id);
        return Result.success("删除成功");
    }
    
    @GetMapping("/username/{username}")
    public Result<StudentDTO> getByUsername(@PathVariable("username") String username) {
        StudentDTO dto = studentService.getByUsername(username);
        if (dto == null) {
            return Result.error(404, "学生信息不存在");
        }
        return Result.success(dto);
    }
    
    /**
     * 通过userId（user_credentials表的id）获取学生信息
     * 用于学生端获取个人信息
     */
    @GetMapping("/user/{userId}")
    public Result<StudentDTO> getByUserId(@PathVariable("userId") Long userId) {
        StudentDTO dto = studentService.getByUserId(userId);
        if (dto == null) {
            return Result.error(404, "学生信息不存在");
        }
        return Result.success(dto);
    }
}

