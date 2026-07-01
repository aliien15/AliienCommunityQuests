package com.aliiensmp.aliienCommunityQuests.database;

import java.util.Set;
import java.util.UUID;

public record ActiveQuestState(int progress, Set<UUID> participants) {
}
