package com.education.selection.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SelectionDTO {
    private Long id;
    private Long studentId;
    private Long courseId;
    private Integer status;
    private String studentName;
    private String studentNo; // 学号
    private String courseName;
    private String courseCode;
    private String teacherName; // 教师姓名
    private BigDecimal credit; // 学分
    private BigDecimal score; // 成绩
    private String coverImage; // 课程封面
    private LocalDateTime selectionTime;
}

