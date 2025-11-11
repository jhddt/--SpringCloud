package com.education.message.service;

import com.education.common.constant.Constants;
import com.education.common.exception.BusinessException;
import com.education.message.constant.MessageConstants;
import com.education.message.enums.MessageType;
import com.education.message.enums.ScopeType;
import com.education.message.feign.CourseServiceClient;
import com.education.message.feign.SelectionServiceClient;
import com.education.message.dto.MessageDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 消息权限验证服务
 * 按照学习通消息权限划分机制实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePermissionService {
    
    private final CourseServiceClient courseServiceClient;
    private final SelectionServiceClient selectionServiceClient;
    private final ObjectMapper objectMapper;
    
    /**
     * 检查用户是否有权限发送消息
     */
    public boolean canSendMessage(Long senderId, String senderRole, MessageDTO messageDTO) {
        String messageType = messageDTO.getMessageType();
        String scopeType = messageDTO.getScopeType();
        Long scopeId = messageDTO.getScopeId();
        
        log.info("检查发送权限: senderId={}, senderRole={}, messageType={}, scopeType={}, scopeId={}", 
                senderId, senderRole, messageType, scopeType, scopeId);
        
        // 1. 检查消息类型权限
        if (!hasMessageTypePermission(senderRole, messageType)) {
            log.warn("用户无权限发送该类型消息: senderId={}, senderRole={}, messageType={}", 
                    senderId, senderRole, messageType);
            throw new BusinessException(403, "无权限发送该类型消息");
        }
        
        // 2. 根据消息类型和范围类型进行具体权限检查
        MessageType type = MessageType.fromCode(messageType);
        ScopeType scope = ScopeType.fromCode(scopeType != null ? scopeType : ScopeType.PRIVATE.getCode());
        
        switch (type) {
            case INSTANT_MESSAGE:
                return canSendInstantMessage(senderId, senderRole, messageDTO, scope, scopeId);
            case SYSTEM_NOTICE:
                return canSendSystemNotice(senderId, senderRole, messageDTO, scope, scopeId);
            case INTERACTION_REMINDER:
                return canSendInteractionReminder(senderId, senderRole, messageDTO);
            case PLATFORM_ANNOUNCEMENT:
                return canSendPlatformAnnouncement(senderId, senderRole, messageDTO);
            default:
                throw new BusinessException(400, "不支持的消息类型");
        }
    }
    
    /**
     * 检查用户是否有权限接收消息
     */
    public boolean canReceiveMessage(Long receiverId, String receiverRole, MessageDTO messageDTO) {
        String scopeType = messageDTO.getScopeType();
        Long scopeId = messageDTO.getScopeId();
        String roleMask = messageDTO.getRoleMask();
        
        log.info("检查接收权限: receiverId={}, receiverRole={}, scopeType={}, scopeId={}, roleMask={}", 
                receiverId, receiverRole, scopeType, scopeId, roleMask);
        
        // 管理员可以查看所有消息
        if (Constants.ROLE_ADMIN.equals(receiverRole)) {
            return true;
        }
        
        ScopeType scope = ScopeType.fromCode(scopeType != null ? scopeType : ScopeType.PRIVATE.getCode());
        
        switch (scope) {
            case PRIVATE:
                return messageDTO.getReceiverId() != null && messageDTO.getReceiverId().equals(receiverId);
            case COURSE:
                return isCourseMember(receiverId, receiverRole, scopeId);
            case GROUP:
                // TODO: 实现群组成员检查
                return true;
            case GLOBAL:
                return hasRolePermission(receiverRole, roleMask);
            default:
                return false;
        }
    }
    
    /**
     * 检查消息类型权限
     */
    private boolean hasMessageTypePermission(String role, String messageType) {
        String[] allowedRoles;
        switch (MessageType.fromCode(messageType)) {
            case INSTANT_MESSAGE:
                allowedRoles = MessageConstants.INSTANT_MESSAGE_ALLOWED_ROLES;
                break;
            case SYSTEM_NOTICE:
                allowedRoles = MessageConstants.SYSTEM_NOTICE_ALLOWED_ROLES;
                break;
            case INTERACTION_REMINDER:
                allowedRoles = MessageConstants.INTERACTION_REMINDER_ALLOWED_ROLES;
                break;
            case PLATFORM_ANNOUNCEMENT:
                allowedRoles = MessageConstants.PLATFORM_ANNOUNCEMENT_ALLOWED_ROLES;
                break;
            default:
                return false;
        }
        return Arrays.asList(allowedRoles).contains(role);
    }
    
    /**
     * 检查是否可以发送即时消息
     */
    private boolean canSendInstantMessage(Long senderId, String senderRole, MessageDTO messageDTO, 
                                         ScopeType scope, Long scopeId) {
        if (scope == ScopeType.PRIVATE) {
            return messageDTO.getReceiverId() != null;
        } else if (scope == ScopeType.COURSE) {
            if (scopeId == null) {
                throw new BusinessException(400, "课程消息必须指定scopeId");
            }
            return isCourseMember(senderId, senderRole, scopeId);
        } else if (scope == ScopeType.GROUP) {
            // TODO: 实现群组成员检查
            return true;
        }
        return false;
    }
    
    /**
     * 检查是否可以发送系统通知
     */
    private boolean canSendSystemNotice(Long senderId, String senderRole, MessageDTO messageDTO, 
                                       ScopeType scope, Long scopeId) {
        if (!Constants.ROLE_TEACHER.equals(senderRole) && !Constants.ROLE_ADMIN.equals(senderRole)) {
            throw new BusinessException(403, "只有教师或管理员可以发送系统通知");
        }
        if (scope != ScopeType.COURSE || scopeId == null) {
            throw new BusinessException(400, "系统通知必须指定课程范围");
        }
        if (Constants.ROLE_TEACHER.equals(senderRole)) {
            return isCourseTeacher(senderId, scopeId);
        }
        return Constants.ROLE_ADMIN.equals(senderRole);
    }
    
    /**
     * 检查是否可以发送互动提醒
     */
    private boolean canSendInteractionReminder(Long senderId, String senderRole, MessageDTO messageDTO) {
        if (!"SYSTEM".equals(senderRole)) {
            throw new BusinessException(403, "互动提醒只能由系统自动生成");
        }
        return true;
    }
    
    /**
     * 检查是否可以发送平台公告
     */
    private boolean canSendPlatformAnnouncement(Long senderId, String senderRole, MessageDTO messageDTO) {
        if (!Constants.ROLE_ADMIN.equals(senderRole)) {
            throw new BusinessException(403, "只有管理员可以发送平台公告");
        }
        return true;
    }
    
    /**
     * 检查用户是否是课程成员
     */
    private boolean isCourseMember(Long userId, String userRole, Long courseId) {
        try {
            if (courseId == null) {
                return false;
            }
            var courseResult = courseServiceClient.getCourseById(courseId);
            if (courseResult == null || courseResult.getCode() != 200 || courseResult.getData() == null) {
                log.warn("课程不存在: courseId={}", courseId);
                return false;
            }
            if (Constants.ROLE_STUDENT.equals(userRole)) {
                try {
                    var selectionResult = selectionServiceClient.checkSelection(userId, courseId);
                    if (selectionResult != null && selectionResult.getCode() == 200) {
                        Boolean isSelected = (Boolean) selectionResult.getData();
                        return Boolean.TRUE.equals(isSelected);
                    }
                } catch (Exception e) {
                    log.warn("检查选课关系失败: userId={}, courseId={}", userId, courseId, e);
                    return false;
                }
            } else if (Constants.ROLE_TEACHER.equals(userRole)) {
                return isCourseTeacher(userId, courseId);
            } else if (Constants.ROLE_ADMIN.equals(userRole)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("检查课程成员关系失败: userId={}, userRole={}, courseId={}", userId, userRole, courseId, e);
            return false;
        }
    }
    
    /**
     * 检查教师是否是课程教师
     */
    private boolean isCourseTeacher(Long teacherId, Long courseId) {
        try {
            var courseResult = courseServiceClient.getCourseById(courseId);
            if (courseResult == null || courseResult.getCode() != 200 || courseResult.getData() == null) {
                return false;
            }
            java.util.Map<String, Object> course = courseResult.getData();
            Object courseTeacherId = course.get("teacherId");
            if (courseTeacherId != null) {
                return teacherId.equals(Long.valueOf(courseTeacherId.toString()));
            }
            return false;
        } catch (Exception e) {
            log.error("检查课程教师关系失败: teacherId={}, courseId={}", teacherId, courseId, e);
            return false;
        }
    }
    
    /**
     * 检查角色权限
     */
    private boolean hasRolePermission(String userRole, String roleMask) {
        if (!StringUtils.hasText(roleMask)) {
            return true; // 没有限制，所有人可见
        }
        
        // 支持两种格式：
        // 1. 简单字符串格式: "TEACHER" 或 "ADMIN,TEACHER,STUDENT"
        // 2. JSON 数组格式: ["TEACHER", "STUDENT"]
        
        // 先尝试简单字符串格式（用逗号分隔）
        if (!roleMask.startsWith("[")) {
            // 简单字符串格式
            return roleMask.contains(userRole);
        }
        
        // JSON 数组格式
        try {
            List<String> allowedRoles = objectMapper.readValue(roleMask, new TypeReference<List<String>>() {});
            return allowedRoles != null && allowedRoles.contains(userRole);
        } catch (Exception e) {
            log.warn("解析角色掩码失败，尝试简单字符串匹配: roleMask={}", roleMask);
            // 如果 JSON 解析失败，回退到简单字符串匹配
            return roleMask.contains(userRole);
        }
    }
}

