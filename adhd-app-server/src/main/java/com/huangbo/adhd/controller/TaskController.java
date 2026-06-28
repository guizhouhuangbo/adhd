package com.huangbo.adhd.controller;

import com.huangbo.adhd.common.Result;
import com.huangbo.adhd.config.AuthContext;
import com.huangbo.adhd.dto.TaskCreateRequest;
import com.huangbo.adhd.dto.TaskView;
import com.huangbo.adhd.service.TaskService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public Result<List<TaskView>> list() {
        return Result.success(taskService.listTasksForUser(AuthContext.getUserId()));
    }

    @PostMapping
    public Result<TaskView> create(@Valid @RequestBody TaskCreateRequest request) {
        return Result.success(taskService.createTask(AuthContext.getUserId(), request));
    }
}
