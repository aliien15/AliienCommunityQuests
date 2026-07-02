package com.aliiensmp.aliienCommunityQuests.config.records;

import com.aliiensmp.aliienCommunityQuests.enums.ObjectiveType;

public record Objective(
        String id,
        ObjectiveType type,
        String target,
        int amount
) {
}
