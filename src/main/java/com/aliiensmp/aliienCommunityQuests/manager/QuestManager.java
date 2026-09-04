package com.aliiensmp.aliienCommunityQuests.manager;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.config.Messages;
import com.aliiensmp.aliienCommunityQuests.config.Quests;
import com.aliiensmp.aliienCommunityQuests.config.Settings;
import com.aliiensmp.aliienCommunityQuests.config.records.ActiveQuestState;
import com.aliiensmp.aliienCommunityQuests.config.records.Objective;
import com.aliiensmp.aliienCommunityQuests.config.records.Quest;
import com.aliiensmp.aliienCommunityQuests.enums.ObjectiveType;
import com.aliiensmp.core.discord.DiscordWebhook;
import com.aliiensmp.core.utils.DebugUtils;
import com.aliiensmp.core.utils.DurationUtils;
import com.aliiensmp.core.utils.MessageUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class QuestManager {

    private final AliienCommunityQuests plugin;

    public static Map<String, ActiveQuestState> ACTIVE_QUESTS = new ConcurrentHashMap<>();

    /**
     * O(1) Lookup cache for the listeners
     * <p>
     * In the internal map, String is the objective target (e.g. IRON_ORE) and the list is all the
     * active objectives that match this objective type and target, allowing for quicker lookups
     */
    public static final EnumMap<ObjectiveType, Map<String, List<ActiveObjectiveContext>>> LISTENER_CACHE = new EnumMap<>(ObjectiveType.class);

    // Record to hold exactly what the listener needs without looking up the quest blueprint
    public record ActiveObjectiveContext(String questId, String objectiveId, int requiredAmount) {}

    public QuestManager(final AliienCommunityQuests plugin) {
        this.plugin = plugin;

        // Pre-fill the EnumMap
        for (ObjectiveType type : ObjectiveType.values()) {
            LISTENER_CACHE.put(type, new ConcurrentHashMap<>());
        }

        plugin.getDatabaseProvider().loadActiveCache().thenAccept(cache -> {

            // Identify and purge ghost quests
            final List<String> invalidQuestIds = cache.keySet().stream()
                    .filter(questId -> Quests.QUEST_LIST.stream().noneMatch(q -> q.id().equals(questId)))
                    .toList();

            invalidQuestIds.forEach(invalidId -> {
                cache.remove(invalidId);
                plugin.getDatabaseProvider().clearActiveQuestBackup(invalidId);
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
                                }
                            });
                        });
            });

            ACTIVE_QUESTS = cache;

            rebuildListenerCache();
        });

        startRotationTask();
        startBackupTask();
    }

    /**
     * Wipes and rebuilds the optimized O(1) listener cache.
     * Must be called anytime quests are generated, loaded, or removed.
     */
    public void rebuildListenerCache() {
        LISTENER_CACHE.values().forEach(Map::clear);

        ACTIVE_QUESTS.forEach((questId, state) -> {
            Quests.QUEST_LIST.stream()
                    .filter(q -> q.id().equals(questId))
                    .findFirst()
                    .ifPresent(quest -> {
                        quest.objectives().stream()
                                .filter(obj -> state.objectiveProgress().containsKey(obj.id()))
                                .forEach(obj -> {
                                    LISTENER_CACHE.get(obj.type())
                                            .computeIfAbsent(obj.target().toUpperCase(Locale.ROOT), k -> new CopyOnWriteArrayList<>())
                                            .add(new ActiveObjectiveContext(questId, obj.id(), obj.amount()));
                                });
                    });
        });
    }

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

    private void startRotationTask() {
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            final long currentTime = System.currentTimeMillis();
            boolean changed = false;

            for (Map.Entry<String, ActiveQuestState> entry : ACTIVE_QUESTS.entrySet()) {
                if (entry.getValue().endTime() <= currentTime) {
                    final String questId = entry.getKey();

                    MessageUtils.broadcast(Messages.PREFIX, Messages.QUEST_ENDED);
                    ACTIVE_QUESTS.remove(questId);
                    plugin.getDatabaseProvider().clearActiveQuestBackup(questId);
                    changed = true;
                }
            }

            if (changed) {
                generateMissingQuests();
            }
        }, 20L, 20L);
    }

    public void generateMissingQuests() {
        boolean questsGenerated = false;

        for (Quest quest : Quests.QUEST_LIST) {
            if (ACTIVE_QUESTS.containsKey(quest.id())) continue;

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

            ActiveQuestState state = new ActiveQuestState(objectivesProgress, participants, timeInMilli);
            ACTIVE_QUESTS.put(quest.id(), state);

            MessageUtils.broadcast(Messages.PREFIX, Messages.QUEST_STARTED);
            questsGenerated = true;

            if (!Settings.WEBHOOK_ENABLED || Settings.WEBHOOK_URL.isEmpty()) continue;
            DiscordWebhook discordWebhook = new DiscordWebhook(Settings.WEBHOOK_URL)
                    .setColor(Settings.WEBHOOK_COLOR)
                    .setTitle(Settings.WEBHOOK_TITLE)
                    .setDescription(Settings.WEBHOOK_DESCRIPTION);

            discordWebhook.sendAsync();
        }

        if (questsGenerated) {
            rebuildListenerCache();
        }
    }
}