package com.education.message.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.education.common.result.Result;
import com.education.message.dto.MessageDTO;
import com.education.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {
    
    private final MessageService messageService;
    
    @PostMapping("/send")
    public Result<MessageDTO> sendMessage(@RequestBody MessageDTO dto) {
        MessageDTO result = messageService.sendMessage(dto);
        return Result.success("发送成功", result);
    }
    
    @GetMapping("/list")
    public Result<Page<MessageDTO>> getMessages(
            @RequestParam("userId") Long userId,
            @RequestParam("otherUserId") Long otherUserId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        Page<MessageDTO> page = messageService.getMessages(userId, otherUserId, current, size);
        return Result.success(page);
    }
    
    @PutMapping("/{id}/read")
    public Result<?> markAsRead(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestParam(value = "userId", required = false) Long userIdParam) {
        log.info("标记消息为已读，消息ID: {}, 请求头X-User-Id: {}, 请求参数userId: {}", id, userIdHeader, userIdParam);
        
        Long userId = null;
        
        // 优先从请求头获取
        if (StringUtils.hasText(userIdHeader)) {
            try {
                userId = Long.parseLong(userIdHeader);
                log.info("从请求头获取到用户ID: {}", userId);
            } catch (NumberFormatException e) {
                log.warn("请求头中的用户ID格式错误: {}", userIdHeader);
                // 忽略，尝试从参数获取
            }
        }
        
        // 如果请求头没有，从请求参数获取（向后兼容）
        if (userId == null && userIdParam != null) {
            userId = userIdParam;
            log.info("从请求参数获取到用户ID: {}", userId);
        }
        
        if (userId == null) {
            log.warn("未找到用户信息，消息ID: {}", id);
            return Result.error("未找到用户信息，请确保已登录");
        }
        
        messageService.markAsRead(id, userId);
        log.info("成功标记消息为已读，消息ID: {}, 用户ID: {}", id, userId);
        return Result.success("已标记为已读");
    }
    
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(@RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        if (StringUtils.hasText(userIdHeader)) {
            try {
                Long userId = Long.parseLong(userIdHeader);
                Long count = messageService.getUnreadCount(userId);
                return Result.success(count);
            } catch (NumberFormatException e) {
                return Result.error("用户ID格式错误");
            }
        }
        return Result.error("未找到用户信息");
    }
    
    @GetMapping("/page")
    public Result<Page<MessageDTO>> getPage(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Role", required = false) String roleHeader,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "senderId", required = false) Long senderId,
            @RequestParam(value = "receiverId", required = false) Long receiverId) {
        Long currentUserId = null;
        if (StringUtils.hasText(userIdHeader)) {
            try {
                currentUserId = Long.parseLong(userIdHeader);
            } catch (NumberFormatException e) {
                // 忽略解析错误，使用null
            }
        }
        Page<MessageDTO> page = messageService.getPage(currentUserId, roleHeader, current, size, keyword, senderId, receiverId);
        return Result.success(page);
    }
}


