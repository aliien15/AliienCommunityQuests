package com.aliiensmp.aliienCommunityQuests.config.records;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public record ActiveQuestState(
        @NotNull Map<String, Integer> objectiveProgress,
        @NotNull Map<UUID, Integer> participantsWithContributions,
        long endTime
) {
}
