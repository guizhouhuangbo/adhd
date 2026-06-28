package com.huangbo.adhd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huangbo.adhd.dto.UserProfileResponse;
import com.huangbo.adhd.dto.DashboardResponse;
import com.huangbo.adhd.dto.TaskView;
import com.huangbo.adhd.entity.User;
import com.huangbo.adhd.mapper.UserMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final TaskService taskService;
    private final AuthService authService;

    public UserService(UserMapper userMapper, TaskService taskService, AuthService authService) {
        this.userMapper = userMapper;
        this.taskService = taskService;
        this.authService = authService;
    }

    public DashboardResponse getDashboard(Long userId) {
        User user = getById(userId);
        List<TaskView> tasks = taskService.listTasksForUser(user.getId());
        return new DashboardResponse(
            user.getNickname(),
            user.getChildName(),
            user.getTotalStars(),
            calculateStreak(tasks),
            "先降低冲突频率，再慢慢提高完成率。今天只盯住一件最重要的小事。",
            tasks
        );
    }

    public UserProfileResponse getProfile(Long userId) {
        return authService.getProfile(userId);
    }

    public User getById(Long userId) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId, userId));
        if (user == null) {
            throw new IllegalStateException("用户不存在");
        }
        return user;
    }

    private int calculateStreak(List<TaskView> tasks) {
        long completedCount = tasks.stream().filter(TaskView::completed).count();
        return (int) completedCount;
    }
}
