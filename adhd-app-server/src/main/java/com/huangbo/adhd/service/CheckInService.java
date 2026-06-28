package com.huangbo.adhd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huangbo.adhd.dto.CheckInRequest;
import com.huangbo.adhd.dto.CheckInResponse;
import com.huangbo.adhd.entity.CheckIn;
import com.huangbo.adhd.entity.Task;
import com.huangbo.adhd.entity.User;
import com.huangbo.adhd.mapper.CheckInMapper;
import com.huangbo.adhd.mapper.TaskMapper;
import com.huangbo.adhd.mapper.UserMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class CheckInService {

    private final CheckInMapper checkInMapper;
    private final TaskMapper taskMapper;
    private final UserMapper userMapper;

    public CheckInService(CheckInMapper checkInMapper, TaskMapper taskMapper, UserMapper userMapper) {
        this.checkInMapper = checkInMapper;
        this.taskMapper = taskMapper;
        this.userMapper = userMapper;
    }

    public CheckInResponse checkIn(Long userId, CheckInRequest request) {
        Task task = taskMapper.selectOne(new LambdaQueryWrapper<Task>()
            .eq(Task::getId, request.taskId())
            .eq(Task::getUserId, userId));
        if (task == null) {
            throw new IllegalStateException("任务不存在");
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        CheckIn existed = checkInMapper.selectOne(new LambdaQueryWrapper<CheckIn>()
            .eq(CheckIn::getUserId, userId)
            .eq(CheckIn::getTaskId, task.getId())
            .ge(CheckIn::getCreatedAt, startOfDay)
            .lt(CheckIn::getCreatedAt, endOfDay)
            .last("limit 1"));
        if (existed != null) {
            throw new IllegalStateException("今天这项任务已经打卡过了");
        }

        CheckIn checkIn = new CheckIn();
        checkIn.setUserId(userId);
        checkIn.setTaskId(task.getId());
        checkIn.setStarsEarned(task.getRewardStars());
        checkIn.setNote(request.note());
        checkIn.setCreatedAt(LocalDateTime.now());
        checkInMapper.insert(checkIn);

        task.setCompleted(true);
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);

        User user = userMapper.selectById(userId);
        user.setTotalStars(user.getTotalStars() + task.getRewardStars());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        return new CheckInResponse(task.getId(), task.getRewardStars(), user.getTotalStars(), "已完成打卡，继续保持");
    }
}
