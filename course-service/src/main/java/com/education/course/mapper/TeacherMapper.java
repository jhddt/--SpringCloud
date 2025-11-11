package com.education.course.mapper;

import com.education.course.entity.Teacher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TeacherMapper {
    
    @Select("SELECT teacher_id, name, department, title, status FROM teacher_service_db.teachers WHERE teacher_id = #{teacherId}")
    Teacher selectById(Long teacherId);
    
    /**
     * 通过userId（user_credentials表的id）获取teacherId
     */
    @Select("SELECT teacher_id FROM user_service_db.user_credentials WHERE id = #{userId}")
    Long selectTeacherIdByUserId(@Param("userId") Long userId);
    
    /**
     * 通过userId获取教师信息（先通过user_credentials获取teacherId，再查询teachers表）
     * 注意：需要确保course-service数据库连接有权限访问user_service_db和teacher_service_db
     */
    @Select("SELECT t.teacher_id, t.name, t.department, t.title, t.status " +
            "FROM teacher_service_db.teachers t " +
            "INNER JOIN user_service_db.user_credentials uc ON t.teacher_id = uc.teacher_id " +
            "WHERE uc.id = #{userId} AND uc.role = 'TEACHER'")
    Teacher selectByUserId(@Param("userId") Long userId);
    
    /**
     * 先通过userId获取teacherId，分步查询（更安全的方式）
     */
    default Teacher selectByUserIdSafe(Long userId) {
        // 先获取teacherId
        Long teacherId = selectTeacherIdByUserId(userId);
        if (teacherId == null) {
            return null;
        }
        // 再通过teacherId查询教师信息
        return selectById(teacherId);
    }
}

