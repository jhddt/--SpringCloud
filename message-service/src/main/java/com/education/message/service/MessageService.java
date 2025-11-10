package com.education.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.education.common.constant.Constants;
import com.education.message.dto.MessageDTO;
import com.education.message.entity.Message;
import com.education.message.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {
    
    private final MessageMapper messageMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Transactional
    public MessageDTO sendMessage(MessageDTO dto) {
        Message message = new Message();
        message.setSenderId(dto.getSenderId());
        message.setSenderType(dto.getSenderType());
        message.setReceiverId(dto.getReceiverId());
        message.setReceiverType(dto.getReceiverType());
        message.setContent(dto.getContent());
        message.setMessageType(dto.getMessageType() != null ? dto.getMessageType() : "TEXT");
        message.setStatus(0);
        message.setCreatedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());
        
        messageMapper.insert(message);
        
        MessageDTO result = new MessageDTO();
        BeanUtils.copyProperties(message, result);
        
        // TODO: 通过服务间调用获取发送者和接收者名称
        // 这里暂时设置为空，后续可以通过 Feign Client 调用 user-service 和 teacher-service
        result.setSenderName("未知用户");
        result.setReceiverName("未知用户");
        
        // 发送WebSocket消息通知
        String channel = "message:" + dto.getReceiverId();
        redisTemplate.convertAndSend(channel, result);
        
        return result;
    }
    
    public Page<MessageDTO> getMessages(Long userId, Long otherUserId, Integer current, Integer size) {
        Page<Message> page = new Page<>(current, size);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.and(w1 -> w1.eq(Message::getSenderId, userId)
                                        .eq(Message::getReceiverId, otherUserId))
                          .or(w2 -> w2.eq(Message::getSenderId, otherUserId)
                                       .eq(Message::getReceiverId, userId)));
        wrapper.orderByDesc(Message::getCreatedAt);
        
        Page<Message> messagePage = messageMapper.selectPage(page, wrapper);
        
        Page<MessageDTO> dtoPage = new Page<>(current, size, messagePage.getTotal());
        List<MessageDTO> dtoList = messagePage.getRecords().stream()
                .map(message -> {
                    MessageDTO dto = new MessageDTO();
                    BeanUtils.copyProperties(message, dto);
                    // TODO: 通过服务间调用获取发送者和接收者名称
                    dto.setSenderName("未知用户");
                    dto.setReceiverName("未知用户");
                    return dto;
                })
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        
        return dtoPage;
    }
    
    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        Message message = messageMapper.selectById(messageId);
        if (message != null && message.getReceiverId() != null && message.getReceiverId().equals(userId)) {
            message.setStatus(1);
            messageMapper.updateById(message);
        }
    }
    
    public Long getUnreadCount(Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, userId)
               .eq(Message::getStatus, 0);
        return messageMapper.selectCount(wrapper);
    }
    
    public Page<MessageDTO> getPage(Long currentUserId, String currentUserRole, Integer current, Integer size, String keyword, Long senderId, Long receiverId) {
        // 如果指定了senderId或receiverId，直接使用（这种情况通常是查看自己发送的或接收的消息）
        if (senderId != null || receiverId != null) {
            Page<Message> page = new Page<>(current, size);
            LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
            if (senderId != null) {
                wrapper.eq(Message::getSenderId, senderId);
            }
            if (receiverId != null) {
                wrapper.eq(Message::getReceiverId, receiverId);
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                wrapper.like(Message::getContent, keyword);
            }
            wrapper.orderByDesc(Message::getCreatedAt);
            
            Page<Message> messagePage = messageMapper.selectPage(page, wrapper);
            
            Page<MessageDTO> dtoPage = new Page<>(current, size, messagePage.getTotal());
            List<MessageDTO> dtoList = messagePage.getRecords().stream()
                    .map(message -> {
                        MessageDTO dto = new MessageDTO();
                        BeanUtils.copyProperties(message, dto);
                        // TODO: 通过服务间调用获取用户名称
                        dto.setSenderName("未知用户");
                        dto.setReceiverName("未知用户");
                        return dto;
                    })
                    .collect(Collectors.toList());
            dtoPage.setRecords(dtoList);
            return dtoPage;
        }
        
        // 如果没有指定senderId和receiverId，需要根据权限过滤
        Page<Message> page = new Page<>(current, size);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        
        if (currentUserId != null && currentUserRole != null) {
            if (Constants.ROLE_ADMIN.equals(currentUserRole)) {
                // 管理员可以看到所有消息
            } else {
                // 非管理员：查询当前用户发送的或接收的消息
                wrapper.and(w -> {
                    w.or(w2 -> w2.eq(Message::getSenderId, currentUserId));
                    w.or(w3 -> w3.eq(Message::getReceiverId, currentUserId));
                });
            }
        }
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(Message::getContent, keyword);
        }
        wrapper.orderByDesc(Message::getCreatedAt);
        
        Page<Message> messagePage = messageMapper.selectPage(page, wrapper);
        
        // 转换为DTO
        List<MessageDTO> filteredList = messagePage.getRecords().stream()
                .map(message -> {
                    MessageDTO dto = new MessageDTO();
                    BeanUtils.copyProperties(message, dto);
                    // TODO: 通过服务间调用获取用户名称
                    dto.setSenderName("未知用户");
                    dto.setReceiverName("未知用户");
                    return dto;
                })
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
}

