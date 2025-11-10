package com.education.teacher.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.education.common.constant.Constants;
import com.education.common.exception.BusinessException;
import com.education.common.result.Result;
import com.education.teacher.dto.TeacherDTO;
import com.education.teacher.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherController {
    
    private final TeacherService teacherService;
    
    @GetMapping("/{id}")
    public Result<TeacherDTO> getById(@PathVariable("id") Long id) {
        TeacherDTO dto = teacherService.getById(id);
        return Result.success(dto);
    }
    
    @GetMapping("/page")
    public Result<Page<TeacherDTO>> getPage(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "department", required = false) String department) {
        Page<TeacherDTO> page = teacherService.getPage(current, size, keyword, department);
        return Result.success(page);
    }
    
    @PostMapping
    public Result<TeacherDTO> create(
            @RequestBody TeacherDTO dto,
            @RequestHeader(value = "X-Role", required = false) String role) {
        if (!StringUtils.hasText(role) || !Constants.ROLE_ADMIN.equals(role)) {
            throw new BusinessException(403, "只有管理员可以创建教师账号");
        }
        TeacherDTO result = teacherService.create(dto);
        return Result.success("创建成功", result);
    }
    
    @PutMapping("/{id}")
    public Result<TeacherDTO> update(
            @PathVariable("id") Long id,
            @RequestBody TeacherDTO dto,
            @RequestHeader(value = "X-Role", required = false) String role) {
        if (!StringUtils.hasText(role) || !Constants.ROLE_ADMIN.equals(role)) {
            throw new BusinessException(403, "只有管理员可以修改教师信息");
        }
        TeacherDTO result = teacherService.update(id, dto);
        return Result.success("更新成功", result);
    }
    
    @DeleteMapping("/{id}")
    public Result<?> delete(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Role", required = false) String role) {
        if (!StringUtils.hasText(role) || !Constants.ROLE_ADMIN.equals(role)) {
            throw new BusinessException(403, "只有管理员可以删除教师账号");
        }
        teacherService.delete(id);
        return Result.success("删除成功");
    }
    
    @PutMapping("/{id}/status")
    public Result<?> updateStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") Integer status,
            @RequestHeader(value = "X-Role", required = false) String role) {
        if (!StringUtils.hasText(role) || !Constants.ROLE_ADMIN.equals(role)) {
            throw new BusinessException(403, "只有管理员可以修改教师状态");
        }
        teacherService.updateStatus(id, status);
        return Result.success("状态更新成功");
    }
    
    /**
     * 通过userId（user_credentials表的id）获取教师信息
     * 用于教师端获取个人信息
     */
    @GetMapping("/user/{userId}")
    public Result<TeacherDTO> getByUserId(@PathVariable("userId") Long userId) {
        TeacherDTO dto = teacherService.getByUserId(userId);
        if (dto == null) {
            return Result.error(404, "教师信息不存在");
        }
        return Result.success(dto);
    }
}
