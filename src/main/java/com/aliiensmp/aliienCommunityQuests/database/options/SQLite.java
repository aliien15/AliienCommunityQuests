package com.aliiensmp.aliienCommunityQuests.database.options;

import com.aliiensmp.aliienCommunityQuests.database.ActiveQuestState;
import com.aliiensmp.aliienCommunityQuests.database.DatabaseProvider;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLite implements DatabaseProvider {

    @Override
    public void init() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public CompletableFuture<Void> grantRewards(Set<UUID> participants, String rewardId) {
        return null;
    }

    @Override
    public CompletableFuture<List<String>> getPendingRewards(UUID playerUuid) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> clearPendingRewards(UUID playerUuid) {
        return null;
    }

    @Override
    public CompletableFuture<Void> saveActiveQuest(String questId, int progress, Set<UUID> participants) {
        return null;
    }

    @Override
    public CompletableFuture<Map<String, ActiveQuestState>> loadActiveCache() {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> clearActiveQuestBackup(String questId) {
        return null;
    }
}
