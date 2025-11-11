package com.education.selection.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.education.common.result.Result;
import com.education.selection.dto.SelectionDTO;
import com.education.selection.service.SelectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 选课控制器
 * 按照流程图实现：
 * 1. 登录验证 - 用户身份 + 角色校验（由网关处理）
 * 2. 拉取可选课程 - 过滤专业/容量/时间
 * 3. 提交选课请求 - 验证冲突/学分/重复选
 * 4. 并发与锁控制 - Redis原子操作
 * 5. 写入数据库 - enrollments、更新course容量
 * 6. 消息推送通知 - 学生、教师、教务同步
 */
@RestController
@RequestMapping("/selection")
@RequiredArgsConstructor
public class SelectionController {
    
    private final SelectionService selectionService;
    
    /**
     * 获取可选课程列表（步骤2：拉取可选课程）
     * 过滤：专业匹配、容量、时间冲突、重复选
     */
    @GetMapping("/available")
    public Result<Page<Map<String, Object>>> getAvailableCourses(
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword) {
        Page<Map<String, Object>> page = selectionService.getAvailableCourses(studentId, current, size, keyword);
        return Result.success(page);
    }
    
    /**
     * 选课（步骤3-6：提交选课请求 -> 并发控制 -> 写入数据库 -> 消息通知）
     */
    @PostMapping("/select")
    public Result<SelectionDTO> selectCourse(
            @RequestParam("studentId") Long studentId,
            @RequestParam("courseId") Long courseId) {
        SelectionDTO dto = selectionService.selectCourse(studentId, courseId);
        return Result.success("选课成功", dto);
    }
    
    /**
     * 获取选课分页列表
     */
    @GetMapping("/page")
    public Result<Page<SelectionDTO>> getPage(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(value = "courseId", required = false) Long courseId) {
        Page<SelectionDTO> page = selectionService.getPage(current, size, studentId, courseId);
        return Result.success(page);
    }
    
    /**
     * 取消选课
     */
    @PutMapping("/{id}/cancel")
    public Result<?> cancel(@PathVariable("id") Long id, @RequestParam("studentId") Long studentId) {
        selectionService.cancel(id, studentId);
        return Result.success("取消选课成功");
    }
    
    /**
     * 更新成绩
     */
    @PutMapping("/{id}/score")
    public Result<?> updateScore(@PathVariable("id") Long id, @RequestParam("score") Double score) {
        selectionService.updateScore(id, score);
        return Result.success("成绩更新成功");
    }
    
    /**
     * 批量更新成绩
     */
    @PutMapping("/batch-score")
    public Result<?> batchUpdateScore(@RequestBody java.util.List<java.util.Map<String, Object>> scoreList) {
        selectionService.batchUpdateScore(scoreList);
        return Result.success("批量更新成绩成功");
    }
}

