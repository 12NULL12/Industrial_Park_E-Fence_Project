package com.fence.service;

import com.fence.dto.task.TaskArriveRequest;
import com.fence.entity.Task;
import com.fence.mapper.TaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class DeliveryService {

    @Autowired
    private TaskMapper taskMapper;

    /**
     * 司机确认送达
     */
    @Transactional
    public String confirmArrive(TaskArriveRequest request) {
        Task task = taskMapper.selectById(request.getTaskId());
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        task.setStatus("ARRIVED");

        if (request.getActualTime() != null && !request.getActualTime().isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            task.setActualEndTime(LocalDateTime.parse(request.getActualTime(), formatter));
        } else {
            task.setActualEndTime(LocalDateTime.now());
        }

        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);

        return "送达成功";
    }
}

