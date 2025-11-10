package com.education.selection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.education.selection.entity.CourseSelection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SelectionMapper extends BaseMapper<CourseSelection> {
    
    @Select("SELECT real_name FROM student WHERE id = #{studentId}")
    String selectStudentName(@Param("studentId") Long studentId);
    
    @Select("SELECT course_name FROM course WHERE id = #{courseId}")
    String selectCourseName(@Param("courseId") Long courseId);
    
    @Select("SELECT course_code FROM course WHERE id = #{courseId}")
    String selectCourseCode(@Param("courseId") Long courseId);
}

