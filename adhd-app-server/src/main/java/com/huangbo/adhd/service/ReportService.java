package com.huangbo.adhd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huangbo.adhd.dto.WeeklyReportResponse;
import com.huangbo.adhd.entity.CheckIn;
import com.huangbo.adhd.mapper.CheckInMapper;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final CheckInMapper checkInMapper;

    public ReportService(CheckInMapper checkInMapper) {
        this.checkInMapper = checkInMapper;
    }

    public WeeklyReportResponse getWeeklyReport(Long userId) {
        int stars = checkInMapper.selectList(new LambdaQueryWrapper<CheckIn>().eq(CheckIn::getUserId, userId)).stream()
            .mapToInt(CheckIn::getStarsEarned)
            .sum();
        int completionRate = Math.min(100, stars * 10 + 20);
        return new WeeklyReportResponse(
            "孩子这周在‘放学后整理书包’上已经能更快进入状态了。",
            "下周继续保留同一个奖励规则，不要频繁改标准，让孩子更容易建立确定感。",
            completionRate,
            stars,
            "最容易卡住的时段是晚饭后到写作业开始前，建议先用 10 分钟过渡任务。"
        );
    }
}
