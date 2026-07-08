package com.aliiensmp.aliienCommunityQuests.config.records;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record ActiveQuestState(
        Map<String, Integer> objectiveProgress,
        Set<UUID> participants,
        long endTime
) {
}
