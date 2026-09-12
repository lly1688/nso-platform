package com.nso.business.task.service;

import java.util.List;
import java.util.Locale;

// 任务类型与项目职责的唯一映射，分配和执行必须复用同一规则。
public final class TaskResponsibilityCatalog {
    private TaskResponsibilityCatalog() {
    }

    public static List<String> forTaskType(String taskType) {
        String normalized = taskType == null ? "" : taskType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PURCHASE" -> List.of("PURCHASER");
            case "PRODUCTION" -> List.of("PRODUCTION");
            case "SAMPLE_PREPARE", "SAMPLE_MAKE", "SAMPLE_REWORK" -> List.of("PRODUCTION", "FIELD_USER");
            case "QUALITY", "INSPECTION" -> List.of("QUALITY");
            case "TECHNICAL", "DESIGN" -> List.of("TECHNICAL");
            case "PROCESS" -> List.of("PROCESS");
            default -> List.of("PROJECT_MANAGER");
        };
    }
}
