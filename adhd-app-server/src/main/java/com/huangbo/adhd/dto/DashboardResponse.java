package com.huangbo.adhd.dto;

import java.util.List;

public record DashboardResponse(String userName, String childName, Integer totalStars, Integer streakDays,
                                String encouragement, List<TaskView> todayTasks) {
}
