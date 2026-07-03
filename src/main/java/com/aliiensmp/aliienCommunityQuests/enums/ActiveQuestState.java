package com.aliiensmp.aliienCommunityQuests.enums;

import java.util.Set;
import java.util.UUID;

public record ActiveQuestState(int progress, Set<UUID> participants) {
}
