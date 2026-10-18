package com.aliiensmp.aliienCommunityQuests.config.records;

import java.util.Map;
import java.util.UUID;

public record ActiveQuestState(
        Map<String, Integer> objectiveProgress,
        Map<UUID, Integer> participantsWithContributions,
        long endTime
) {
}
