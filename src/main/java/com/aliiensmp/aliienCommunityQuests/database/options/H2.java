package com.aliiensmp.aliienCommunityQuests.database.options;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.database.ActiveQuestState;
import com.aliiensmp.aliienCommunityQuests.database.DatabaseProvider;
import com.aliiensmp.core.AliienCore;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class H2 implements DatabaseProvider {

    private final AliienCommunityQuests plugin;

    public H2(final AliienCommunityQuests plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        String createActiveQuests = """
            CREATE TABLE IF NOT EXISTS active_quests (
                quest_id VARCHAR(255) PRIMARY KEY,
                progress INT NOT NULL
            );""";

        String createParticipants = """
            CREATE TABLE IF NOT EXISTS active_quest_participants (
                quest_id VARCHAR(255) NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                PRIMARY KEY (quest_id, player_uuid)
            );""";

        String createRewards = """
            CREATE TABLE IF NOT EXISTS unclaimed_rewards (
                player_uuid VARCHAR(36) NOT NULL,
                reward_id VARCHAR(255) NOT NULL,
                INDEX(player_uuid)
            );""";

        AliienCore.getDatabase().executeAsync(createActiveQuests);
        AliienCore.getDatabase().executeAsync(createParticipants);
        AliienCore.getDatabase().executeAsync(createRewards);
    }

    @Override
    public CompletableFuture<List<String>> getPendingRewards(final UUID playerUuid) {
        String query = "SELECT reward_id FROM unclaimed_rewards WHERE player_uuid = ?;";

        return AliienCore.getDatabase().queryAsync(query, rs -> {
            List<String> rewards = new ArrayList<>();

            try {
                while (rs.next()) {
                    rewards.add(rs.getString("reward_id"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return rewards;
        }, playerUuid.toString());
    }

    @Override
    public CompletableFuture<Void> grantRewards(final Set<UUID> participants, final String rewardId) {
        String query = "INSERT INTO unclaimed_rewards (player_uuid, reward_id) VALUES (?, ?);";

        List<CompletableFuture<Boolean>> futures = participants.stream()
                .map(uuid -> AliienCore.getDatabase().executeAsync(query, uuid.toString(), rewardId))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Override
    public CompletableFuture<Boolean> clearPendingRewards(final UUID playerUuid) {
        String query = "DELETE FROM unclaimed_rewards WHERE player_uuid = ?;";

        return AliienCore.getDatabase().executeAsync(query, playerUuid.toString());
    }

    @Override
    public CompletableFuture<Void> saveActiveQuest(final String questId, final int progress, final Set<UUID> participants) {
        String updateProgress = "MERGE INTO active_campaigns (quest_id, progress) KEY(quest_id) VALUES (?, ?);";

        String insertParticipant = "MERGE INTO active_campaign_participants (quest_id, player_uuid) KEY(quest_id, player_uuid) VALUES (?, ?);";

        CompletableFuture<Boolean> progressFuture = AliienCore.getDatabase().executeAsync(updateProgress, questId, progress);

        List<CompletableFuture<Boolean>> participantFutures = participants.stream()
                .map(uuid -> AliienCore.getDatabase().executeAsync(insertParticipant, questId, uuid.toString()))
                .toList();

        ArrayList<CompletableFuture<?>> allFutures = new ArrayList<>();
        allFutures.add(progressFuture);
        allFutures.addAll(participantFutures);

        return CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]));
    }

    @Override
    public CompletableFuture<Map<String, ActiveQuestState>> loadActiveCache() {
        String query = "SELECT c.quest_id, c.progress, p.player_uuid " +
                "FROM active_campaigns c " +
                "LEFT JOIN active_campaign_participants p ON c.quest_id = p.quest_id;";

        return AliienCore.getDatabase().queryAsync(query, rs -> {
            Map<String, ActiveQuestState> cache = new java.util.concurrent.ConcurrentHashMap<>();

            try {
                while (rs.next()) {
                    String questId = rs.getString("quest_id");
                    int progress = rs.getInt("progress");
                    String uuidString = rs.getString("player_uuid");

                    cache.computeIfAbsent(questId, k -> new ActiveQuestState(progress, java.util.concurrent.ConcurrentHashMap.newKeySet()));

                    java.util.Optional.ofNullable(uuidString).ifPresent(uuid ->
                            cache.get(questId).participants().add(UUID.fromString(uuid))
                    );
                }
            } catch (java.sql.SQLException e) {
                e.printStackTrace();
            }

            return cache;
        });
    }

    @Override
    public CompletableFuture<Boolean> clearActiveQuestBackup(final String questId) {
        String deleteProgress = "DELETE FROM active_campaigns WHERE quest_id = ?;";
        String deleteParticipants = "DELETE FROM active_campaign_participants WHERE quest_id = ?;";

        CompletableFuture<Boolean> progressFuture = AliienCore.getDatabase().executeAsync(deleteProgress, questId);
        CompletableFuture<Boolean> participantsFuture = AliienCore.getDatabase().executeAsync(deleteParticipants, questId);

        return progressFuture.thenCombine(participantsFuture, (progressDeleted, participantsDeleted) ->
                progressDeleted && participantsDeleted
        );
    }
}
