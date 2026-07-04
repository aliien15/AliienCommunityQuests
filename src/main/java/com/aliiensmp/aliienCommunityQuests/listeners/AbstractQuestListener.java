package com.aliiensmp.aliienCommunityQuests.listeners;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.config.Messages;
import com.aliiensmp.aliienCommunityQuests.config.Quests;
import com.aliiensmp.aliienCommunityQuests.config.records.Objective;
import com.aliiensmp.aliienCommunityQuests.config.records.Quest;
import com.aliiensmp.aliienCommunityQuests.config.records.ActiveQuestState;
import com.aliiensmp.aliienCommunityQuests.enums.ObjectiveType;
import com.aliiensmp.core.utils.MessageUtils;
import org.bukkit.event.Listener;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.aliiensmp.aliienCommunityQuests.manager.QuestManager.ACTIVE_QUESTS;

public abstract class AbstractQuestListener implements Listener {

    protected final AliienCommunityQuests plugin;

    public AbstractQuestListener(final AliienCommunityQuests plugin) {
        this.plugin = plugin;
    }

    /**
     * Provesses progress for any active community quest by incrementing the progress in 1 unit.
     *
     * @param playerUUID The UUID of the player contributing.
     * @param type The type of objective being completed.
     * @param target The target string (e.g., Block name, Entity name).
     */
    protected void handleProgress(final UUID playerUUID, final ObjectiveType type, final String target) {
        handleProgress(playerUUID, type, target, 1);
    }

    /**
     * Processes progress for any active community quest.
     *
     * @param playerUUID The UUID of the player contributing.
     * @param type The type of objective being completed.
     * @param target The target string (e.g., Block name, Entity name).
     * @param amount the amount to increment the current progress by.
     */
    protected void handleProgress(final UUID playerUUID, final ObjectiveType type, final String target, final int amount) {
        // If not quests are currently active
        if (ACTIVE_QUESTS.isEmpty()) return;

        for (final String questId : ACTIVE_QUESTS.keySet()) {

            // Get the Quest object that corresponds to the active quest we are looking up
            Quest quest = null;
            for (final Quest q : Quests.QUEST_LIST) {
                if (q.id().equals(questId)) {
                    quest = q;
                    break;
                }
            }
            if (quest == null) continue;

            // Look through all the objectives this quest requires to check if the player's action matches one of them.
            Objective targetObjective = null;
            for (final Objective obj : quest.objectives()) {
                if (obj.type() == type && obj.target().equals(target)) {
                    targetObjective = obj;
                    break;
                }
            }
            if (targetObjective == null) continue;

            // Update the progress
            final Quest finalQuest = quest;
            final Objective finalTargetObjective = targetObjective;

            ACTIVE_QUESTS.computeIfPresent(questId, (id, state) -> {
                final int newProgress = state.progress() + amount;
                state.participants().add(playerUUID);

                if (newProgress >= finalTargetObjective.amount()) {
                    MessageUtils.broadcast(Messages.PREFIX, Messages.QUEST_COMPLETED);

                    CompletableFuture.runAsync(() -> {
                        plugin.getDatabaseProvider().clearActiveQuestBackup(id);
                        for (final String reward : finalQuest.rewards()) {
                            plugin.getDatabaseProvider().grantRewards(state.participants(), reward);
                        }
                    });

                    return null;
                }

                return new ActiveQuestState(newProgress, state.participants());
            });
        }
    }
}