package com.education.course.entity;

import lombok.Data;

@Data
public class Teacher {
    private Long teacherId;
    private String name;
    private String department;
    private String title;
    private Integer status;
}

