package com.education.selection.feign;

import com.education.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 消息服务Feign客户端
 * 用于发送选课通知
 * 注意：消息服务需要从请求头获取用户信息，但系统通知可以通过特殊方式发送
 */
@FeignClient(name = "message-service", path = "/message")
public interface MessageServiceClient {
    
    /**
     * 发送消息（系统通知）
     * 注意：由于消息服务需要从请求头获取用户信息，这里我们需要通过请求头传递
     * 对于系统通知，可以使用特殊的方式
     */
    @PostMapping("/send")
    Result<?> sendMessage(
            @RequestHeader(value = "X-User-Id", required = false) Long senderId,
            @RequestHeader(value = "X-Role", required = false) String senderRole,
            @RequestBody Map<String, Object> messageData);
}

