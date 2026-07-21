package com.aliiensmp.aliienCommunityQuests.database.options;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.config.records.ActiveQuestState;
import com.aliiensmp.aliienCommunityQuests.database.DatabaseProvider;
import com.aliiensmp.core.AliienCore;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SQLite implements DatabaseProvider {

    private final AliienCommunityQuests plugin;
    private final String prefix;

    public SQLite(final AliienCommunityQuests plugin, final String prefix) {
        this.plugin = plugin;
        this.prefix = prefix;
    }

    @Override
    public void init() {
        String createObjectives = """
            CREATE TABLE IF NOT EXISTS %sactive_quest_objectives (
                quest_id VARCHAR(255) NOT NULL,
                objective_id VARCHAR(255) NOT NULL,
                progress INT NOT NULL,
                end_time BIGINT NOT NULL,
                PRIMARY KEY (quest_id, objective_id)
            );""".formatted(prefix);

        String createParticipants = """
            CREATE TABLE IF NOT EXISTS %sactive_quest_participants (
                quest_id VARCHAR(255) NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                PRIMARY KEY (quest_id, player_uuid)
            );""".formatted(prefix);

        String createRewards = """
            CREATE TABLE IF NOT EXISTS %sunclaimed_rewards (
                player_uuid VARCHAR(36) NOT NULL,
                reward_id VARCHAR(255) NOT NULL,
                INDEX(player_uuid)
            );""".formatted(prefix);

        AliienCore.getDatabase().executeAsync(createObjectives);
        AliienCore.getDatabase().executeAsync(createParticipants);
        AliienCore.getDatabase().executeAsync(createRewards);
    }

    @Override
    public CompletableFuture<List<String>> getPendingRewards(final UUID playerUuid) {
        String query = "SELECT reward_id FROM " + prefix + "unclaimed_rewards WHERE player_uuid = ?;";

        return AliienCore.getDatabase().queryAsync(query, rs -> {
            List<String> rewards = new ArrayList<>();
            try {
                while (rs.next()) rewards.add(rs.getString("reward_id"));
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return rewards;
        }, playerUuid.toString());
    }

    @Override
    public CompletableFuture<Void> grantRewards(final Set<UUID> participants, final String rewardId) {
        String query = "INSERT INTO " + prefix + "unclaimed_rewards (player_uuid, reward_id) VALUES (?, ?);";

        List<CompletableFuture<Boolean>> futures = participants.stream()
                .map(uuid -> AliienCore.getDatabase().executeAsync(query, uuid.toString(), rewardId))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Override
    public CompletableFuture<Boolean> clearPendingRewards(final UUID playerUuid) {
        String query = "DELETE FROM " + prefix + "unclaimed_rewards WHERE player_uuid = ?;";
        return AliienCore.getDatabase().executeAsync(query, playerUuid.toString());
    }

    @Override
    public CompletableFuture<Void> saveActiveQuest(final String questId, final Map<String, Integer> objectiveProgress, final Set<UUID> participants, final long endTime) {
        String updateProgress = "INSERT INTO " + prefix + "active_quest_objectives (quest_id, objective_id, progress, end_time) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT(quest_id, objective_id) DO UPDATE SET progress = excluded.progress, end_time = excluded.end_time;";

        String insertParticipant = "INSERT OR IGNORE INTO " + prefix + "active_quest_participants (quest_id, player_uuid) VALUES (?, ?);";

        ArrayList<CompletableFuture<?>> allFutures = new ArrayList<>();

        objectiveProgress.forEach((objId, progress) -> {
            allFutures.add(AliienCore.getDatabase().executeAsync(updateProgress, questId, objId, progress, endTime));
        });

        participants.forEach(uuid -> {
            allFutures.add(AliienCore.getDatabase().executeAsync(insertParticipant, questId, uuid.toString()));
        });

        return CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]));
    }

    @Override
    public CompletableFuture<Map<String, ActiveQuestState>> loadActiveCache() {
        String query = "SELECT o.quest_id, o.objective_id, o.progress, o.end_time, p.player_uuid " +
                "FROM " + prefix + "active_quest_objectives o " +
                "LEFT JOIN " + prefix + "active_quest_participants p ON o.quest_id = p.quest_id;";

        return AliienCore.getDatabase().queryAsync(query, rs -> {
            Map<String, ActiveQuestState> cache = new ConcurrentHashMap<>();

            try {
                while (rs.next()) {
                    String questId = rs.getString("quest_id");
                    String objectiveId = rs.getString("objective_id");
                    int progress = rs.getInt("progress");
                    long endTime = rs.getLong("end_time");
                    String uuidString = rs.getString("player_uuid");

                    cache.computeIfAbsent(questId, k -> new ActiveQuestState(new ConcurrentHashMap<>(), ConcurrentHashMap.newKeySet(), endTime));

                    cache.get(questId).objectiveProgress().put(objectiveId, progress);

                    Optional.ofNullable(uuidString).ifPresent(uuid ->
                            cache.get(questId).participants().add(UUID.fromString(uuid))
                    );
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return cache;
        });
    }

    @Override
    public CompletableFuture<Boolean> clearActiveQuestBackup(final String questId) {
        String deleteProgress = "DELETE FROM " + prefix + "active_quest_objectives WHERE quest_id = ?;";
        String deleteParticipants = "DELETE FROM " + prefix + "active_quest_participants WHERE quest_id = ?;";

        CompletableFuture<Boolean> progressFuture = AliienCore.getDatabase().executeAsync(deleteProgress, questId);
        CompletableFuture<Boolean> participantsFuture = AliienCore.getDatabase().executeAsync(deleteParticipants, questId);

        return progressFuture.thenCombine(participantsFuture, (progressDeleted, participantsDeleted) -> progressDeleted && participantsDeleted);
    }
}