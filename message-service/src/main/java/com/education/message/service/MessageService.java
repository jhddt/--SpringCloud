package com.education.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.education.common.constant.Constants;
import com.education.common.exception.BusinessException;
import com.education.message.constant.MessageConstants;
import com.education.message.dto.MessageDTO;
import com.education.message.entity.Message;
import com.education.message.enums.MessageType;
import com.education.message.enums.ScopeType;
import com.education.message.feign.StudentServiceClient;
import com.education.message.feign.TeacherServiceClient;
import com.education.message.mapper.MessageMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息服务
 * 按照学习通消息权限划分机制重构
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {
    
    private final MessageMapper messageMapper;
    private final MessagePermissionService permissionService;
    private final StudentServiceClient studentServiceClient;
    private final TeacherServiceClient teacherServiceClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * 发送消息（带权限验证）
     */
    @Transactional
    public MessageDTO sendMessage(Long senderId, String senderRole, MessageDTO dto) {
        log.info("发送消息: senderId={}, senderRole={}, messageType={}, scopeType={}, scopeId={}", 
                senderId, senderRole, dto.getMessageType(), dto.getScopeType(), dto.getScopeId());
        
        // 1. 权限验证
        permissionService.canSendMessage(senderId, senderRole, dto);
        
        // 2. 设置发送者信息
        dto.setSenderId(senderId);
        dto.setSenderType(senderRole);
        
        // 3. 设置默认值
        if (!StringUtils.hasText(dto.getContentType())) {
            dto.setContentType("TEXT");
        }
        if (!StringUtils.hasText(dto.getScopeType())) {
            dto.setScopeType(ScopeType.PRIVATE.getCode());
        }
        if (!StringUtils.hasText(dto.getMessageType())) {
            dto.setMessageType(MessageType.INSTANT_MESSAGE.getCode());
        }
        
        // 4. 设置角色掩码
        if (!StringUtils.hasText(dto.getRoleMask())) {
            dto.setRoleMask(generateRoleMask(dto));
        }
        
        // 5. 保存消息
        Message message = new Message();
        BeanUtils.copyProperties(dto, message);
        message.setStatus(MessageConstants.MESSAGE_STATUS_UNREAD);
        message.setCreatedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());
        
        messageMapper.insert(message);
        
        // 6. 填充发送者和接收者名称
        MessageDTO result = convertToDTO(message);
        fillUserNames(result);
        
        // 7. 发送WebSocket通知
        sendWebSocketNotification(result);
        
        return result;
    }
    
    /**
     * 获取消息列表（带权限过滤）
     */
    public Page<MessageDTO> getMessages(Long userId, Long otherUserId, Integer current, Integer size) {
        Page<Message> page = new Page<>(current, size);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.and(w1 -> w1.eq(Message::getSenderId, userId)
                                        .eq(Message::getReceiverId, otherUserId))
                          .or(w2 -> w2.eq(Message::getSenderId, otherUserId)
                                       .eq(Message::getReceiverId, userId)));
        wrapper.eq(Message::getScopeType, ScopeType.PRIVATE.getCode());
        wrapper.orderByDesc(Message::getCreatedAt);
        
        Page<Message> messagePage = messageMapper.selectPage(page, wrapper);
        
        Page<MessageDTO> dtoPage = new Page<>(current, size, messagePage.getTotal());
        List<MessageDTO> dtoList = messagePage.getRecords().stream()
                .map(this::convertToDTO)
                .peek(this::fillUserNames)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        
        return dtoPage;
    }
    
    /**
     * 获取消息分页（带权限过滤和数据隔离）
     */
    public Page<MessageDTO> getPage(Long currentUserId, String currentUserRole, Integer current, Integer size, 
                                    String keyword, String messageType, String scopeType, Long scopeId) {
        log.info("=== 获取消息分页 === userId={}, role={}, current={}, size={}, keyword={}, messageType={}, scopeType={}, scopeId={}", 
                currentUserId, currentUserRole, current, size, keyword, messageType, scopeType, scopeId);
        
        Page<Message> page = new Page<>(current, size);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        
        // 1. 数据隔离：根据用户角色和范围过滤
        if (currentUserId != null && currentUserRole != null) {
            if (Constants.ROLE_ADMIN.equals(currentUserRole)) {
                // 管理员可以看到所有消息
                log.info("✓ 管理员权限：可以查看所有消息");
            } else {
                // 非管理员：只能看到自己发送的、接收的、或有权限查看的范围消息（课程/群组/全局）
                wrapper.and(w -> {
                    // 自己发送或接收的消息
                    w.or(w1 -> w1.eq(Message::getSenderId, currentUserId))
                     .or(w2 -> w2.eq(Message::getReceiverId, currentUserId));
                    
                    // 课程消息：检查是否是课程成员（当查询指定了课程范围时）
                    if (scopeType != null && ScopeType.COURSE.getCode().equals(scopeType) && scopeId != null) {
                        w.or(w3 -> w3.eq(Message::getScopeType, ScopeType.COURSE.getCode())
                                     .eq(Message::getScopeId, scopeId)
                                     .like(Message::getRoleMask, currentUserRole));
                    }
                    
                    // 全局公告：根据角色掩码显示（receiverId 可能为 null）
                    w.or(w4 -> w4.eq(Message::getScopeType, ScopeType.GLOBAL.getCode())
                                 .like(Message::getRoleMask, currentUserRole));
                });
            }
        }
        
        // 2. 消息类型过滤
        if (StringUtils.hasText(messageType)) {
            wrapper.eq(Message::getMessageType, messageType);
        }
        
        // 3. 范围类型过滤
        if (StringUtils.hasText(scopeType)) {
            wrapper.eq(Message::getScopeType, scopeType);
        }
        
        // 4. 范围ID过滤
        if (scopeId != null) {
            wrapper.eq(Message::getScopeId, scopeId);
        }
        
        // 5. 关键词搜索
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Message::getContent, keyword);
        }
        
        wrapper.orderByDesc(Message::getCreatedAt);
        
        Page<Message> messagePage = messageMapper.selectPage(page, wrapper);
        
        // 6. 转换为DTO并过滤权限
        // 管理员直接使用数据库分页结果，不需要二次过滤和分页
        if (Constants.ROLE_ADMIN.equals(currentUserRole)) {
            log.info("✓ 管理员分支：查询到 {} 条消息，总数 {}", messagePage.getRecords().size(), messagePage.getTotal());
            List<MessageDTO> dtoList = messagePage.getRecords().stream()
                    .map(this::convertToDTO)
                    .peek(this::fillUserNames)
                    .collect(Collectors.toList());
            
            Page<MessageDTO> dtoPage = new Page<>(current, size, messagePage.getTotal());
            dtoPage.setRecords(dtoList);
            log.info("✓ 管理员返回：{} 条消息", dtoList.size());
            return dtoPage;
        }
        
        // 非管理员需要进行权限过滤
        List<MessageDTO> filteredList = messagePage.getRecords().stream()
                .map(this::convertToDTO)
                .filter(dto -> {
                    // 再次验证接收权限
                    try {
                        return permissionService.canReceiveMessage(currentUserId, currentUserRole, dto);
                    } catch (Exception e) {
                        log.warn("权限验证失败: messageId={}, error={}", dto.getMessageId(), e.getMessage());
                        return false;
                    }
                })
                .peek(this::fillUserNames)
                .collect(Collectors.toList());
        
        // 手动分页
        int start = (current - 1) * size;
        int end = Math.min(start + size, filteredList.size());
        List<MessageDTO> pagedList = start < filteredList.size() 
                ? filteredList.subList(start, end) 
                : new ArrayList<>();
        
        Page<MessageDTO> dtoPage = new Page<>(current, size, filteredList.size());
        dtoPage.setRecords(pagedList);
        
        return dtoPage;
    }
    
    /**
     * 标记消息为已读
     */
    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        Message message = messageMapper.selectById(messageId);
        if (message != null && message.getReceiverId() != null && message.getReceiverId().equals(userId)) {
            message.setStatus(MessageConstants.MESSAGE_STATUS_READ);
            message.setUpdatedAt(LocalDateTime.now());
            messageMapper.updateById(message);
        } else {
            throw new BusinessException(403, "无权限标记该消息为已读");
        }
    }
    
    /**
     * 获取未读消息数
     */
    public Long getUnreadCount(Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, userId)
               .eq(Message::getStatus, MessageConstants.MESSAGE_STATUS_UNREAD);
        return messageMapper.selectCount(wrapper);
    }
    
    /**
     * 生成角色掩码
     */
    private String generateRoleMask(MessageDTO dto) {
        try {
            String scopeType = dto.getScopeType();
            String messageType = dto.getMessageType();
            
            List<String> roles = new ArrayList<>();
            
            if (ScopeType.COURSE.getCode().equals(scopeType)) {
                // 课程消息：默认所有课程成员可见
                roles.add(Constants.ROLE_STUDENT);
                roles.add(Constants.ROLE_TEACHER);
            } else if (ScopeType.GLOBAL.getCode().equals(scopeType)) {
                // 全局消息：根据消息类型设置
                if (MessageType.PLATFORM_ANNOUNCEMENT.getCode().equals(messageType)) {
                    roles.add(Constants.ROLE_STUDENT);
                    roles.add(Constants.ROLE_TEACHER);
                    roles.add(Constants.ROLE_ADMIN);
                }
            } else {
                // 私聊和群组消息：不设置角色掩码
                return null;
            }
            
            return objectMapper.writeValueAsString(roles);
        } catch (Exception e) {
            log.error("生成角色掩码失败", e);
            return null;
        }
    }
    
    /**
     * 填充用户名称
     */
    private void fillUserNames(MessageDTO dto) {
        try {
            // 填充发送者名称
            if (dto.getSenderId() != null && StringUtils.hasText(dto.getSenderType())) {
                dto.setSenderName(getUserName(dto.getSenderId(), dto.getSenderType()));
            }
            
            // 填充接收者名称
            if (dto.getReceiverId() != null && StringUtils.hasText(dto.getReceiverType())) {
                if (!"GROUP".equals(dto.getReceiverType())) {
                    dto.setReceiverName(getUserName(dto.getReceiverId(), dto.getReceiverType()));
                }
            } else if ("GLOBAL".equals(dto.getScopeType())) {
                // 全局公告：根据 roleMask 设置接收者名称
                String roleMask = dto.getRoleMask();
                if (StringUtils.hasText(roleMask)) {
                    if (roleMask.equals("TEACHER")) {
                        dto.setReceiverName("全体教师");
                    } else if (roleMask.equals("STUDENT")) {
                        dto.setReceiverName("全体学生");
                    } else if (roleMask.contains("ADMIN") && roleMask.contains("TEACHER") && roleMask.contains("STUDENT")) {
                        dto.setReceiverName("全体用户");
                    } else {
                        dto.setReceiverName("指定用户组");
                    }
                } else {
                    dto.setReceiverName("全体用户");
                }
            }
        } catch (Exception e) {
            log.error("填充用户名称失败", e);
            // 只在未设置时设置默认值
            if (dto.getSenderName() == null) {
                dto.setSenderName("未知用户");
            }
            if (dto.getReceiverName() == null) {
                dto.setReceiverName("未知用户");
            }
        }
    }
    
    /**
     * 获取用户名称（带缓存）
     */
    @Cacheable(value = "user:name", key = "#userId + ':' + #userType", unless = "#result == null || #result == '未知用户'")
    private String getUserName(Long userId, String userType) {
        try {
            if (Constants.ROLE_STUDENT.equals(userType)) {
                // userId 是 user_credentials 的ID，需走 /student/user/{userId}
                var result = studentServiceClient.getStudentByUserId(userId);
                if (result != null && result.getCode() == 200 && result.getData() != null) {
                    Object data = result.getData();
                    if (data instanceof java.util.Map<?, ?> map) {
                        Object name = map.get("name");
                        Object username = map.get("username");
                        if (name != null && !name.toString().trim().isEmpty()) {
                            return name.toString();
                        }
                        if (username != null && !username.toString().trim().isEmpty()) {
                            return username.toString();
                        }
                    }
                }
            } else if (Constants.ROLE_TEACHER.equals(userType)) {
                // userId 是 user_credentials 的ID，需走 /teacher/user/{userId}
                var result = teacherServiceClient.getTeacherByUserId(userId);
                if (result != null && result.getCode() == 200 && result.getData() != null) {
                    Object data = result.getData();
                    if (data instanceof java.util.Map<?, ?> map) {
                        Object name = map.get("name");
                        Object username = map.get("username");
                        if (name != null && !name.toString().trim().isEmpty()) {
                            return name.toString();
                        }
                        if (username != null && !username.toString().trim().isEmpty()) {
                            return username.toString();
                        }
                    }
                }
            } else if (Constants.ROLE_ADMIN.equals(userType)) {
                return "管理员";
            }
        } catch (Exception e) {
            log.error("获取用户名称失败: userId={}, userType={}", userId, userType, e);
        }
        return "未知用户";
    }
    
    /**
     * 发送WebSocket通知（异步）
     */
    @Async
    private void sendWebSocketNotification(MessageDTO message) {
        try {
            String channel = MessageConstants.REDIS_MESSAGE_PREFIX + message.getReceiverId();
            redisTemplate.convertAndSend(channel, message);
            
            // 如果是课程消息，还需要发送到课程频道
            if (ScopeType.COURSE.getCode().equals(message.getScopeType()) && message.getScopeId() != null) {
                String courseChannel = MessageConstants.WS_CHANNEL_COURSE_PREFIX + message.getScopeId();
                redisTemplate.convertAndSend(courseChannel, message);
            }
        } catch (Exception e) {
            log.error("发送WebSocket通知失败", e);
        }
    }
    
    /**
     * 转换为DTO
     */
    private MessageDTO convertToDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        BeanUtils.copyProperties(message, dto);
        return dto;
    }
    
    /**
     * 兼容旧接口：发送消息（不带权限验证，已废弃）
     */
    @Deprecated
    @Transactional
    public MessageDTO sendMessage(MessageDTO dto) {
        // 兼容旧接口，但应该从请求头获取senderId和senderRole
        throw new BusinessException(400, "请使用新的发送接口，需要提供senderId和senderRole");
    }
    
    /**
     * 兼容旧接口：获取消息分页（已废弃）
     */
    @Deprecated
    public Page<MessageDTO> getPage(Long currentUserId, String currentUserRole, Integer current, Integer size, 
                                    String keyword, Long senderId, Long receiverId) {
        // 兼容旧接口，转换为新接口参数
        return getPage(currentUserId, currentUserRole, current, size, keyword, null, null, null);
    }
}
