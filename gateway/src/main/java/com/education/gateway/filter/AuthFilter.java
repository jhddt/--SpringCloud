package com.education.gateway.filter;

import com.education.common.constant.Constants;
import com.education.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/file/download",
            "/api/file/view"  // 文件查看接口，允许匿名访问
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String fullUri = request.getURI().toString();

        // 打印所有请求路径（用于调试）
        log.info("=== 网关过滤器执行 === path={}, fullUri={}", path, fullUri);

        // WebSocket 连接需要特殊处理（SockJS 会先请求 /info 端点）
        // 路径格式：/api/message/ws 或 /api/message/ws/info
        boolean isWebSocket = path.startsWith("/api/message/ws");
        
        // 白名单直接放行
        // 文件查看和下载接口允许匿名访问，上传接口需要认证
        boolean isWhiteList = path.startsWith("/api/file/view")  // 文件查看接口（优先检查）
                || path.startsWith("/api/file/download")  // 文件下载接口
                || isWebSocket  // WebSocket 连接（允许匿名，但实际使用时需要认证）
                || WHITE_LIST.stream().anyMatch(path::startsWith);  // 其他白名单路径
        
        log.info("白名单检查结果: isWhiteList={}, path={}", isWhiteList, path);
        
        if (isWhiteList) {
            log.info("✓ 白名单路径，直接放行: path={}, fullUri={}", path, fullUri);
            return chain.filter(exchange);
        }
        
        log.info("✗ 需要认证的路径: path={}, fullUri={}", path, fullUri);

        String token = request.getHeaders().getFirst(Constants.TOKEN_HEADER);
        if (!StringUtils.hasText(token) || !token.startsWith(Constants.TOKEN_PREFIX)) {
            log.warn("未找到有效的 Token，返回 401: path={}", path);
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        token = token.substring(Constants.TOKEN_PREFIX.length());
        if (!jwtUtil.validateToken(token)) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        // 将用户信息添加到请求头
        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);
            
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-Username", username)
                    .header("X-Role", role)
                    .build();
            
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        } catch (Exception e) {
            log.error("Token解析失败", e);
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}

