package com.education.course.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.education.common.result.Result;
import com.education.course.dto.CourseDTO;
import com.education.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class CourseController {
    
    private final CourseService courseService;
    
    @GetMapping("/{id}")
    public Result<CourseDTO> getById(@PathVariable("id") Long id) {
        CourseDTO dto = courseService.getById(id);
        return Result.success(dto);
    }
    
    @GetMapping("/page")
    public Result<Page<CourseDTO>> getPage(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status) {
        Page<CourseDTO> page = courseService.getPage(current, size, keyword, status);
        return Result.success(page);
    }
    
    @PostMapping
    public Result<CourseDTO> create(@Valid @RequestBody CourseDTO dto) {
        CourseDTO result = courseService.create(dto);
        return Result.success("创建成功", result);
    }
    
    @PutMapping("/{id}")
    public Result<CourseDTO> update(@PathVariable("id") Long id, @Valid @RequestBody CourseDTO dto) {
        CourseDTO result = courseService.update(id, dto);
        return Result.success("更新成功", result);
    }
    
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable("id") Long id) {
        courseService.delete(id);
        return Result.success("删除成功");
    }
}

