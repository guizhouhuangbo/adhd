package com.huangbo.adhd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huangbo.adhd.dto.TaskCreateRequest;
import com.huangbo.adhd.dto.TaskView;
import com.huangbo.adhd.integration.ModelClient;
import com.huangbo.adhd.entity.Task;
import com.huangbo.adhd.mapper.TaskMapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    private final TaskMapper taskMapper;
    private final ModelClient modelClient;

    public TaskService(TaskMapper taskMapper, ModelClient modelClient) {
        this.taskMapper = taskMapper;
        this.modelClient = modelClient;
    }

    public List<TaskView> listTasksForUser(Long userId) {
        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
            .eq(Task::getUserId, userId)
            .orderByDesc(Task::getId));

        if (tasks.isEmpty()) {
            seedDefaultTasks(userId);
            tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .orderByDesc(Task::getId));
        }

        return tasks.stream().map(this::toView).toList();
    }

    public TaskView createTask(Long userId, TaskCreateRequest request) {
        List<String> steps = splitTask(request.name());

        Task task = new Task();
        task.setUserId(userId);
        task.setName(request.name());
        task.setRewardStars(2);
        task.setScheduleLabel("今晚 19:30");
        task.setStepsJson(String.join("||", steps));
        task.setCompleted(false);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);
        return toView(task);
    }

    private TaskView toView(Task task) {
        List<String> steps = Arrays.stream(task.getStepsJson().split("\\|\\|"))
            .filter(step -> !step.isBlank())
            .toList();
        String stepsText = steps.stream().collect(Collectors.joining(" / "));
        return new TaskView(
            task.getId(),
            task.getName(),
            task.getRewardStars(),
            task.getScheduleLabel(),
            steps,
            stepsText,
            Boolean.TRUE.equals(task.getCompleted())
        );
    }

    private void seedDefaultTasks(Long userId) {
        createSeedTask(userId, "放学后整理书包", 2, "17:30", "拿出作业本||把书本按明天课程装好||检查铅笔盒和水杯||完成后贴一颗星");
        createSeedTask(userId, "睡前洗澡并收玩具", 3, "20:10", "先收 5 件玩具||去浴室前关掉电子屏幕||洗澡 10 分钟||穿好睡衣后给表扬");
    }

    private void createSeedTask(Long userId, String name, int stars, String scheduleLabel, String steps) {
        Task task = new Task();
        task.setUserId(userId);
        task.setName(name);
        task.setRewardStars(stars);
        task.setScheduleLabel(scheduleLabel);
        task.setStepsJson(steps);
        task.setCompleted(false);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);
    }

    private List<String> splitTask(String taskName) {
        try {
            String content = modelClient.chat(
                "你是 ADHD 儿童行为管理助手，请把家长输入的大任务拆成 4 条具体、短句、可执行的小步骤，每行一条，不要加编号。",
                taskName
            );
            List<String> steps = Arrays.stream(content.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .map(line -> line.replaceFirst("^[0-9]+[.)、\\s-]*", ""))
                .toList();
            if (!steps.isEmpty()) {
                return steps;
            }
        } catch (Exception ignored) {
        }
        return List.of(
            "先告诉孩子现在只做第一步",
            "把需要的物品提前准备好",
            "专注 10 分钟完成当前小目标",
            "完成后马上表扬并发星星"
        );
    }
}
