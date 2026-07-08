package com.aliiensmp.aliienCommunityQuests.manager;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.config.Quests;
import com.aliiensmp.aliienCommunityQuests.config.Settings;
import com.aliiensmp.aliienCommunityQuests.config.records.ActiveQuestState;
import com.aliiensmp.aliienCommunityQuests.config.records.Objective;
import com.aliiensmp.aliienCommunityQuests.config.records.Quest;
import com.aliiensmp.core.utils.DurationUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestManager {

    private final AliienCommunityQuests plugin;

    public static Map<String, ActiveQuestState> ACTIVE_QUESTS = new ConcurrentHashMap<>();

    public QuestManager(final AliienCommunityQuests plugin) {
        this.plugin = plugin;

        plugin.getDatabaseProvider().loadActiveCache().thenAccept(cache -> {
            ACTIVE_QUESTS = cache;
            plugin.getLogger().info("Successfully loaded " + ACTIVE_QUESTS.size() + " active quests into memory.");
        });

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
     * Creates a new quest
     */
    public void generateNewQuest() {
        Optional<Quest> nextQuest = Quests.QUEST_LIST.stream()
                .filter(quest -> !ACTIVE_QUESTS.containsKey(quest.id()))
                .sorted(Comparator.comparingInt(Quest::priority))
                .findFirst();

        nextQuest.ifPresent(quest -> {
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

            plugin.getDatabaseProvider().saveActiveQuest(quest.id(), objectivesProgress, participants, timeInMilli);

            // Update live map
            ActiveQuestState state = new ActiveQuestState(objectivesProgress, participants, timeInMilli);
            ACTIVE_QUESTS.put(quest.id(), state);
        });
    }
}