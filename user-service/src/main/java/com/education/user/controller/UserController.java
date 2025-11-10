package com.education.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.education.common.result.Result;
import com.education.user.dto.StudentDTO;
import com.education.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/{id}")
    public Result<StudentDTO> getById(@PathVariable("id") Long id) {
        StudentDTO dto = userService.getById(id);
        return Result.success(dto);
    }
    
    @GetMapping("/page")
    public Result<Page<StudentDTO>> getPage(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "major", required = false) String major,
            @RequestParam(value = "grade", required = false) String grade) {
        Page<StudentDTO> page = userService.getPage(current, size, keyword, major, grade);
        return Result.success(page);
    }
    
    @PostMapping
    public Result<StudentDTO> create(@RequestBody StudentDTO dto) {
        StudentDTO result = userService.create(dto);
        return Result.success("创建成功", result);
    }
    
    @PutMapping("/{id}")
    public Result<StudentDTO> update(@PathVariable("id") Long id, @RequestBody StudentDTO dto) {
        StudentDTO result = userService.update(id, dto);
        return Result.success("更新成功", result);
    }
    
    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable("id") Long id, @RequestParam("status") Integer status) {
        userService.updateStatus(id, status);
        return Result.success("状态更新成功");
    }
}
