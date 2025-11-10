package com.education.selection.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SelectionDTO {
    private Long id;
    private Long studentId;
    private Long courseId;
    private Integer status;
    private String studentName;
    private String courseName;
    private String courseCode;
    private LocalDateTime selectionTime;
}

