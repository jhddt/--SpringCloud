package com.education.gateway.config;

import com.education.gateway.exception.GatewayExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 网关异常处理配置
 */
@Configuration
public class GatewayExceptionConfig {

    @Bean
    @Primary
    public ErrorWebExceptionHandler errorWebExceptionHandler() {
        return new GatewayExceptionHandler();
    }
}

