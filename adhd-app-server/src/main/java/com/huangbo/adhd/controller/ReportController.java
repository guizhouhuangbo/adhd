package com.huangbo.adhd.controller;

import com.huangbo.adhd.common.Result;
import com.huangbo.adhd.config.AuthContext;
import com.huangbo.adhd.dto.WeeklyReportResponse;
import com.huangbo.adhd.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/weekly")
    public Result<WeeklyReportResponse> weekly() {
        return Result.success(reportService.getWeeklyReport(AuthContext.getUserId()));
    }
}
