package com.education.message.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.education.common.result.Result;
import com.education.message.dto.MessageDTO;
import com.education.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 消息控制器
 * 按照学习通消息权限划分机制重构
 */
@Slf4j
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {
    
    private final MessageService messageService;
    
    /**
     * 发送消息（新接口，带权限验证）
     */
    @PostMapping("/send")
    public Result<MessageDTO> sendMessage(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Role", required = false) String roleHeader,
            @RequestBody MessageDTO dto) {
        
        // 从请求头获取用户信息
        Long senderId = parseUserId(userIdHeader);
        String senderRole = roleHeader;
        
        if (senderId == null || !StringUtils.hasText(senderRole)) {
            return Result.error("未找到用户信息，请确保已登录");
        }
        
        MessageDTO result = messageService.sendMessage(senderId, senderRole, dto);
        return Result.success("发送成功", result);
    }
    
    /**
     * 获取私聊消息列表
     */
    @GetMapping("/list")
    public Result<Page<MessageDTO>> getMessages(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestParam("otherUserId") Long otherUserId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        
        Long userId = parseUserId(userIdHeader);
        if (userId == null) {
            return Result.error("未找到用户信息，请确保已登录");
        }
        
        Page<MessageDTO> page = messageService.getMessages(userId, otherUserId, current, size);
        return Result.success(page);
    }
    
    /**
     * 标记消息为已读
     */
    @PutMapping("/{id}/read")
    public Result<?> markAsRead(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        
        Long userId = parseUserId(userIdHeader);
        if (userId == null) {
            return Result.error("未找到用户信息，请确保已登录");
        }
        
        messageService.markAsRead(id, userId);
        return Result.success("已标记为已读");
    }
    
    /**
     * 获取未读消息数
     */
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(@RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = parseUserId(userIdHeader);
        if (userId == null) {
            return Result.error("未找到用户信息，请确保已登录");
        }
        
        Long count = messageService.getUnreadCount(userId);
        return Result.success(count);
    }
    
    /**
     * 获取消息分页（带权限过滤和数据隔离）
     */
    @GetMapping("/page")
    public Result<Page<MessageDTO>> getPage(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-Role", required = false) String roleHeader,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "messageType", required = false) String messageType,
            @RequestParam(value = "scopeType", required = false) String scopeType,
            @RequestParam(value = "scopeId", required = false) Long scopeId) {
        
        Long currentUserId = parseUserId(userIdHeader);
        
        Page<MessageDTO> page = messageService.getPage(currentUserId, roleHeader, current, size, 
                keyword, messageType, scopeType, scopeId);
        return Result.success(page);
    }
    
    /**
     * 解析用户ID
     */
    private Long parseUserId(String userIdHeader) {
        if (StringUtils.hasText(userIdHeader)) {
            try {
                return Long.parseLong(userIdHeader);
            } catch (NumberFormatException e) {
                log.warn("用户ID格式错误: {}", userIdHeader);
            }
        }
        return null;
    }
}
