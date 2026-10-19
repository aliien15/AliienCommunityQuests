package com.aliiensmp.aliienCommunityQuests.config.records;

import com.aliiensmp.aliienCommunityQuests.enums.ObjectiveType;
import org.jetbrains.annotations.NotNull;

public record Objective(
        @NotNull String id,
        @NotNull ObjectiveType type,
        @NotNull String target,
        int amount
) {
}
