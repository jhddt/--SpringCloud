package com.education.teacher;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.education.teacher.mapper")
public class TeacherServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TeacherServiceApplication.class, args);
    }
}

