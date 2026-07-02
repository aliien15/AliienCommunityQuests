package com.aliiensmp.aliienCommunityQuests.manager;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.config.Settings;
import com.aliiensmp.aliienCommunityQuests.database.ActiveQuestState;
import com.aliiensmp.core.utils.DurationUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QuestManager {

    private final AliienCommunityQuests plugin;

    public static Map<String, ActiveQuestState> ACTIVE_QUESTS = new ConcurrentHashMap<>();

    public QuestManager(AliienCommunityQuests plugin) {
        this.plugin = plugin;

        plugin.getDatabaseProvider().loadActiveCache().thenAccept(cache -> {
            ACTIVE_QUESTS = cache;
            plugin.getLogger().info("Successfully loaded " + ACTIVE_QUESTS.size() + " active quests into memory.");
        });

        startBackupTask();
    }

    /**
     * Backs up all the data from the active quests to the database
     */
    private void startBackupTask() {
        plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            ACTIVE_QUESTS.keySet().forEach(questId -> {
                plugin.getDatabaseProvider().saveActiveQuest(
                        questId,
                        ACTIVE_QUESTS.get(questId).progress(),
                        ACTIVE_QUESTS.get(questId).participants());
            });
        }, 20L, DurationUtils.toTicks(Settings.BACKUP_INTERVAL));
    }
}