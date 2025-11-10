package com.education.selection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_selection")
public class CourseSelection {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long courseId;
    private Integer status; // 0-待审核，1-已通过，2-已拒绝
    private LocalDateTime selectionTime;
    private LocalDateTime approveTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

