package com.education.selection.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.education.common.result.Result;
import com.education.selection.dto.SelectionDTO;
import com.education.selection.service.SelectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/selection")
@RequiredArgsConstructor
public class SelectionController {
    
    private final SelectionService selectionService;
    
    @PostMapping("/select")
    public Result<SelectionDTO> selectCourse(
            @RequestParam("studentId") Long studentId,
            @RequestParam("courseId") Long courseId) {
        SelectionDTO dto = selectionService.selectCourse(studentId, courseId);
        return Result.success("选课申请已提交", dto);
    }
    
    @GetMapping("/page")
    public Result<Page<SelectionDTO>> getPage(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(value = "courseId", required = false) Long courseId) {
        Page<SelectionDTO> page = selectionService.getPage(current, size, studentId, courseId);
        return Result.success(page);
    }
    
    @PutMapping("/{id}/cancel")
    public Result<?> cancel(@PathVariable("id") Long id, @RequestParam("studentId") Long studentId) {
        selectionService.cancel(id, studentId);
        return Result.success("取消选课成功");
    }
}

