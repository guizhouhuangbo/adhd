package com.huangbo.adhd.controller;

import com.huangbo.adhd.common.Result;
import com.huangbo.adhd.config.AuthContext;
import com.huangbo.adhd.dto.DashboardResponse;
import com.huangbo.adhd.dto.UserProfileResponse;
import com.huangbo.adhd.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me/dashboard")
    public Result<DashboardResponse> dashboard() {
        return Result.success(userService.getDashboard(AuthContext.getUserId()));
    }

    @GetMapping("/me")
    public Result<UserProfileResponse> me() {
        return Result.success(userService.getProfile(AuthContext.getUserId()));
    }
}
