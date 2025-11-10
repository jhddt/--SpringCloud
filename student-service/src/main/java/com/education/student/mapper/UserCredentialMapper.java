package com.education.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.education.student.entity.UserCredential;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserCredentialMapper extends BaseMapper<UserCredential> {
    
    @Insert("INSERT INTO user_credentials (student_id, teacher_id, username, password_hash, email, phone, role, status, created_at, updated_at) " +
            "VALUES (#{studentId}, #{teacherId}, #{username}, #{passwordHash}, #{email}, #{phone}, #{role}, 1, NOW(), NOW())")
    void insertCredential(@Param("studentId") Long studentId,
                         @Param("teacherId") Long teacherId,
                         @Param("username") String username,
                         @Param("passwordHash") String passwordHash,
                         @Param("email") String email,
                         @Param("phone") String phone,
                         @Param("role") String role);
    
    @Select("SELECT * FROM user_credentials WHERE username = #{username}")
    UserCredential selectByUsername(@Param("username") String username);
    
    @Select("SELECT * FROM user_credentials WHERE teacher_id = #{teacherId}")
    UserCredential selectByTeacherId(@Param("teacherId") Long teacherId);
    
    @org.apache.ibatis.annotations.Update("UPDATE user_credentials SET password_hash = #{passwordHash}, updated_at = NOW() WHERE id = #{id}")
    void updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);
    
    @Select("SELECT id FROM user_credentials WHERE username = #{username}")
    Long selectIdByUsername(@Param("username") String username);
    
    @Select("SELECT * FROM user_credentials WHERE student_id = #{studentId}")
    UserCredential selectByStudentId(@Param("studentId") Long studentId);
}

