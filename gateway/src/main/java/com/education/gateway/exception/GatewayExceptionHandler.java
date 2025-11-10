package com.education.gateway.exception;

import com.education.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 网关全局异常处理器（WebFlux）
 */
@Slf4j
@Order(-2) // 优先级高于默认的 DefaultErrorWebExceptionHandler
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        
        // 过滤静态资源相关的错误（favicon.ico、根路径等）
        String path = exchange.getRequest().getPath().value();
        if (shouldIgnore(path, ex)) {
            response.setStatusCode(HttpStatus.NOT_FOUND);
            return response.setComplete();
        }

        // 记录其他异常
        log.error("网关异常：路径={}, 异常={}", path, ex.getMessage(), ex);

        // 设置响应状态码
        if (ex instanceof ResponseStatusException) {
            ResponseStatusException statusException = (ResponseStatusException) ex;
            response.setStatusCode(statusException.getStatusCode());
        } else {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 设置响应头
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 构建错误响应
        Result<?> result = Result.error("系统异常：" + ex.getMessage());
        
        return response.writeWith(Mono.fromSupplier(() -> {
            DataBufferFactory bufferFactory = response.bufferFactory();
            try {
                String json = com.alibaba.fastjson2.JSON.toJSONString(result);
                return bufferFactory.wrap(json.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                log.error("序列化错误响应失败", e);
                return bufferFactory.wrap("{\"code\":500,\"message\":\"系统异常\"}".getBytes(StandardCharsets.UTF_8));
            }
        }));
    }

    /**
     * 判断是否应该忽略该错误
     */
    private boolean shouldIgnore(String path, Throwable ex) {
        // 忽略静态资源请求
        if (path.equals("/") || 
            path.equals("/favicon.ico") || 
            path.startsWith("/static/") ||
            path.startsWith("/assets/") ||
            path.endsWith(".ico") ||
            path.endsWith(".png") ||
            path.endsWith(".jpg") ||
            path.endsWith(".css") ||
            path.endsWith(".js")) {
            log.debug("忽略静态资源请求：{}", path);
            return true;
        }

        // 忽略 NoResourceFoundException（静态资源未找到）
        String exceptionName = ex.getClass().getSimpleName();
        if (exceptionName.contains("NoResourceFoundException")) {
            // 检查是否是静态资源相关的异常
            String message = ex.getMessage();
            if (message != null && (
                message.contains("static resource") ||
                message.contains("favicon") ||
                message.contains(".ico") ||
                message.contains(".png") ||
                message.contains(".css") ||
                message.contains(".js")
            )) {
                log.debug("忽略静态资源异常：路径={}, 异常={}", path, message);
                return true;
            }
            // 处理无效的 API 路径（如 /api 或 /api/xxx 但不在路由配置中）
            if (path.equals("/api") || (path.startsWith("/api/") && message != null && message.contains("static resource"))) {
                log.debug("忽略无效 API 路径：路径={}", path);
                return true;
            }
        }

        return false;
    }
}

