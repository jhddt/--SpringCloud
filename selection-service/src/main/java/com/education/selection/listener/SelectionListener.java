package com.education.selection.listener;

import com.education.selection.dto.SelectionDTO;
import com.education.selection.service.SelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SelectionListener {
    
    private final SelectionService selectionService;
    
    @RabbitListener(queues = "selection.queue")
    public void handleSelection(SelectionDTO dto) {
        try {
            log.info("处理选课消息：studentId={}, courseId={}", dto.getStudentId(), dto.getCourseId());
            selectionService.processSelection(dto);
        } catch (Exception e) {
            log.error("处理选课消息失败", e);
        }
    }
}

