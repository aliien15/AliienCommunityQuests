package com.aliiensmp.aliienCommunityQuests.database.options;

import com.aliiensmp.aliienCommunityQuests.config.records.ActiveQuestState;
import com.aliiensmp.aliienCommunityQuests.database.DatabaseProvider;
import com.aliiensmp.core.AliienCore;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MariaDB implements DatabaseProvider {

    private final String prefix;

    public MariaDB(final String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void init() {
        final String createObjectives = """
            CREATE TABLE IF NOT EXISTS %sactive_quest_objectives (
                quest_id VARCHAR(255) NOT NULL,
                objective_id VARCHAR(255) NOT NULL,
                progress INT NOT NULL,
                end_time BIGINT NOT NULL,
                PRIMARY KEY (quest_id, objective_id)
            );""".formatted(prefix);

        final String createParticipants = """
            CREATE TABLE IF NOT EXISTS %sactive_quest_participants (
                quest_id VARCHAR(255) NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                player_contribution INT NOT NULL DEFAULT 0,
                PRIMARY KEY (quest_id, player_uuid)
            );""".formatted(prefix);

        final String createRewards = """
            CREATE TABLE IF NOT EXISTS %sunclaimed_rewards (
                player_uuid VARCHAR(36) NOT NULL,
                reward_id VARCHAR(255) NOT NULL,
                INDEX(player_uuid)
            );""".formatted(prefix);

        final CompletableFuture<Boolean> objectivesFuture = AliienCore.getDatabase().executeAsync(createObjectives);
        final CompletableFuture<Boolean> participantsFuture = AliienCore.getDatabase().executeAsync(createParticipants);
        final CompletableFuture<Boolean> rewardsFuture = AliienCore.getDatabase().executeAsync(createRewards);

        // Handle conversions if needed
        participantsFuture.thenRun(() -> AliienCore.getDatabase().executeAsync(
                "ALTER TABLE %sactive_quest_participants ADD COLUMN IF NOT EXISTS player_contribution INT NOT NULL DEFAULT 0;".formatted(prefix)
        ));
    }

    @Override
    public CompletableFuture<List<String>> getPendingRewards(final @NotNull UUID playerUuid) {
        final String query = "SELECT reward_id FROM " + prefix + "unclaimed_rewards WHERE player_uuid = ?;";

        return AliienCore.getDatabase().queryAsync(query, rs -> {
            final List<String> rewards = new ArrayList<>();
            try {
                while (rs.next()) rewards.add(rs.getString("reward_id"));
            } catch (final SQLException e) {
                e.printStackTrace();
            }
            return rewards;
        }, playerUuid.toString());
    }

    @Override
    public CompletableFuture<Void> grantRewards(final @NotNull Set<UUID> participants, final @NotNull String rewardId) {
        final String query = "INSERT INTO " + prefix + "unclaimed_rewards (player_uuid, reward_id) VALUES (?, ?);";

        final List<CompletableFuture<Boolean>> futures = participants.stream()
                .map(uuid -> AliienCore.getDatabase().executeAsync(query, uuid.toString(), rewardId))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Override
    public CompletableFuture<Boolean> clearPendingRewards(final @NotNull UUID playerUuid) {
        final String query = "DELETE FROM " + prefix + "unclaimed_rewards WHERE player_uuid = ?;";
        return AliienCore.getDatabase().executeAsync(query, playerUuid.toString());
    }

    @Override
    public CompletableFuture<Void> saveActiveQuest(final @NotNull String questId, final @NotNull Map<String, Integer> objectiveProgress, final @NotNull Map<UUID, Integer> participantsWithContributions, final long endTime) {
        final String updateProgress = "INSERT INTO " + prefix + "active_quest_objectives (quest_id, objective_id, progress, end_time) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE progress = VALUES(progress), end_time = VALUES(end_time);";

        final String insertParticipant = "INSERT INTO " + prefix + "active_quest_participants (quest_id, player_uuid, player_contribution) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE player_contribution = VALUES(player_contribution);";

        final List<CompletableFuture<?>> allFutures = new ArrayList<>();

        objectiveProgress.forEach((objId, progress) -> {
            allFutures.add(AliienCore.getDatabase().executeAsync(updateProgress, questId, objId, progress, endTime));
        });

        participantsWithContributions.forEach((uuid, contribution) -> {
            allFutures.add(AliienCore.getDatabase().executeAsync(insertParticipant, questId, uuid.toString(), contribution));
        });

        return CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]));
    }

    @Override
    public CompletableFuture<Map<String, ActiveQuestState>> loadActiveCache() {
        final String query = "SELECT o.quest_id, o.objective_id, o.progress, o.end_time, p.player_uuid, p.player_contribution " +
                "FROM " + prefix + "active_quest_objectives o " +
                "LEFT JOIN " + prefix + "active_quest_participants p ON o.quest_id = p.quest_id;";

        return AliienCore.getDatabase().queryAsync(query, rs -> {
            final Map<String, ActiveQuestState> cache = new ConcurrentHashMap<>();

            try {
                while (rs.next()) {
                    final String questId = rs.getString("quest_id");
                    final String objectiveId = rs.getString("objective_id");
                    final int progress = rs.getInt("progress");
                    final long endTime = rs.getLong("end_time");
                    final String uuidString = rs.getString("player_uuid");
                    final int playerContribution = rs.getInt("player_contribution");

                    cache.computeIfAbsent(questId, k -> new ActiveQuestState(new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), endTime));

                    cache.get(questId).objectiveProgress().put(objectiveId, progress);

                    Optional.ofNullable(uuidString).ifPresent(uuid ->
                            cache.get(questId).participantsWithContributions().put(UUID.fromString(uuid), playerContribution)
                    );
                }
            } catch (final SQLException e) {
                e.printStackTrace();
            }

            return cache;
        });
    }

    @Override
    public CompletableFuture<Boolean> clearActiveQuestBackup(final @NotNull String questId) {
        final String deleteProgress = "DELETE FROM " + prefix + "active_quest_objectives WHERE quest_id = ?;";
        final String deleteParticipants = "DELETE FROM " + prefix + "active_quest_participants WHERE quest_id = ?;";

        final CompletableFuture<Boolean> progressFuture = AliienCore.getDatabase().executeAsync(deleteProgress, questId);
        final CompletableFuture<Boolean> participantsFuture = AliienCore.getDatabase().executeAsync(deleteParticipants, questId);

        return progressFuture.thenCombine(participantsFuture, (progressDeleted, participantsDeleted) -> progressDeleted && participantsDeleted);
    }
}