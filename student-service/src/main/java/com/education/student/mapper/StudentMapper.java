package com.education.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.education.student.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {
    
    @Select("SELECT student_id FROM students WHERE name = #{name} AND major = #{major} LIMIT 1")
    Long selectStudentIdByNameAndMajor(@Param("name") String name, @Param("major") String major);
}

