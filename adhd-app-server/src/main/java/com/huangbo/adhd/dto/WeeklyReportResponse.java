package com.huangbo.adhd.dto;

public record WeeklyReportResponse(String highlight, String suggestion, Integer completionRate,
                                   Integer starsEarned, String hardestMoment) {
}
