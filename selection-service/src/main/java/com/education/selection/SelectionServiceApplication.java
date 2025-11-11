package com.education.selection;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableRabbit
@EnableFeignClients
@MapperScan("com.education.selection.mapper")
public class SelectionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SelectionServiceApplication.class, args);
    }
}

