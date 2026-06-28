package com.huangbo.adhd.dto;

import java.util.List;

public record TaskView(Long id, String name, Integer rewardStars, String scheduleLabel, List<String> steps, String stepsText,
                       boolean completed) {
}
