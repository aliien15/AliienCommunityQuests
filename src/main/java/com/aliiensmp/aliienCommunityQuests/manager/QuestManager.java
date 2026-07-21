package com.aliiensmp.aliienCommunityQuests.manager;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.config.Messages;
import com.aliiensmp.aliienCommunityQuests.config.Quests;
import com.aliiensmp.aliienCommunityQuests.config.Settings;
import com.aliiensmp.aliienCommunityQuests.config.records.ActiveQuestState;
import com.aliiensmp.aliienCommunityQuests.config.records.Objective;
import com.aliiensmp.aliienCommunityQuests.config.records.Quest;
import com.aliiensmp.core.utils.DurationUtils;
import com.aliiensmp.core.utils.MessageUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class QuestManager {

    private final AliienCommunityQuests plugin;

    public static Map<String, ActiveQuestState> ACTIVE_QUESTS = new ConcurrentHashMap<>();

    public QuestManager(final AliienCommunityQuests plugin) {
        this.plugin = plugin;

        plugin.getDatabaseProvider().loadActiveCache().thenAccept(cache -> {

            // Identify and purge ghost quests
            final List<String> invalidQuestIds = cache.keySet().stream()
                    .filter(questId -> Quests.QUEST_LIST.stream().noneMatch(q -> q.id().equals(questId)))
                    .toList();

            invalidQuestIds.forEach(invalidId -> {
                cache.remove(invalidId);
                CompletableFuture.runAsync(() -> plugin.getDatabaseProvider().clearActiveQuestBackup(invalidId));
                plugin.getLogger().warning("Cleaned up ghost quest '" + invalidId + "' from the database as it is missing from quests.yml.");
            });

            // Correct soft-locks if the owner lowered the required objective amounts
            cache.forEach((questId, state) -> {
                Quests.QUEST_LIST.stream()
                        .filter(q -> q.id().equals(questId))
                        .findFirst()
                        .ifPresent(blueprint -> {
                            blueprint.objectives().forEach(obj -> {
                                int currentProgress = state.objectiveProgress().getOrDefault(obj.id(), 0);
                                if (currentProgress > obj.amount()) {
                                    state.objectiveProgress().put(obj.id(), obj.amount());
                                    plugin.getLogger().info("Capped progress for objective '" + obj.id() + "' in quest '" + questId + "' due to a configuration change.");
                                }
                            });
                        });
            });

            ACTIVE_QUESTS = cache;
            plugin.getLogger().info("Successfully loaded " + ACTIVE_QUESTS.size() + " active quests into memory.");
        });

        startRotationTask();
        startBackupTask();
    }

    /**
     * Backs up all the data from the active quests to the database at fixed intervals.
     */
    private void startBackupTask() {
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {

            ACTIVE_QUESTS.forEach((questId, state) -> {
                plugin.getDatabaseProvider().saveActiveQuest(
                        questId,
                        state.objectiveProgress(),
                        state.participants(),
                        state.endTime()
                );
            });

        }, 20L, DurationUtils.toTicks(Settings.BACKUP_INTERVAL));
    }

    /**
     * Timer that keeps an eye on the quests, handling if any of them reach their deadline
     * without being completed by the players
     */
    private void startRotationTask() {
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            final long currentTime = System.currentTimeMillis();

            for (Map.Entry<String, ActiveQuestState> entry : ACTIVE_QUESTS.entrySet()) {
                if (entry.getValue().endTime() <= currentTime) {
                    final String questId = entry.getKey();

                    MessageUtils.broadcast(Messages.PREFIX, Messages.QUEST_ENDED);
                    ACTIVE_QUESTS.remove(questId);
                    CompletableFuture.runAsync(() -> plugin.getDatabaseProvider().clearActiveQuestBackup(questId));
                    generateMissingQuests();
                }
            }
        }, 20L, 20L);
    }

    /**
     * Scans the configuration for any inactive quests and starts them all simultaneously.
     */
    public void generateMissingQuests() {
        Quests.QUEST_LIST.stream()
                .filter(quest -> !ACTIVE_QUESTS.containsKey(quest.id()))
                .forEach(quest -> {
                    // Update DB
                    final String timeString = quest.duration();
                    final long timeInMilli = DurationUtils.parse(timeString).toMillis() + System.currentTimeMillis();

                    final Map<String, Integer> objectivesProgress = new ConcurrentHashMap<>();

                    List<Objective> availableObjectives = new ArrayList<>(quest.objectives());
                    Collections.shuffle(availableObjectives);
                    availableObjectives.stream()
                            .limit(quest.objectivesAmount())
                            .forEach(objective -> objectivesProgress.put(objective.id(), 0));

                    final Set<UUID> participants = ConcurrentHashMap.newKeySet();

                    CompletableFuture.runAsync(() -> plugin.getDatabaseProvider().saveActiveQuest(quest.id(), objectivesProgress, participants, timeInMilli));

                    // Update live map
                    ActiveQuestState state = new ActiveQuestState(objectivesProgress, participants, timeInMilli);
                    ACTIVE_QUESTS.put(quest.id(), state);

                    // Other
                    MessageUtils.broadcast(Messages.PREFIX, Messages.QUEST_STARTED);
                });
    }
}