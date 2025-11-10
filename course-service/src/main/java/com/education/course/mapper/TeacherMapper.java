package com.education.course.mapper;

import com.education.course.entity.Teacher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TeacherMapper {
    
    @Select("SELECT teacher_id, name, department, title, status FROM teacher_service_db.teachers WHERE teacher_id = #{teacherId}")
    Teacher selectById(Long teacherId);
}

