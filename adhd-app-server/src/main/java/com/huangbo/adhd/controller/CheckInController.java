package com.huangbo.adhd.controller;

import com.huangbo.adhd.common.Result;
import com.huangbo.adhd.config.AuthContext;
import com.huangbo.adhd.dto.CheckInRequest;
import com.huangbo.adhd.dto.CheckInResponse;
import com.huangbo.adhd.service.CheckInService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkins")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @PostMapping
    public Result<CheckInResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        return Result.success(checkInService.checkIn(AuthContext.getUserId(), request));
    }
}
