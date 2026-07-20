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
     * Processes progress for any active community quest by incrementing the progress in 1 unit.
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
        if (ACTIVE_QUESTS.isEmpty()) return;

        for (final String questId : ACTIVE_QUESTS.keySet()) {

            // Get the Quest object
            Quest quest = Quests.QUEST_LIST.stream()
                    .filter(q -> q.id().equals(questId))
                    .findFirst()
                    .orElse(null);
            if (quest == null) continue;

            // Find the matching objective
            Objective targetObjective = quest.objectives().stream()
                    .filter(obj -> obj.type() == type && obj.target().equals(target))
                    .findFirst()
                    .orElse(null);
            if (targetObjective == null) continue;

            // Check if the objective is already maxed out before updating
            int currentPreProgress = ACTIVE_QUESTS.get(questId).objectiveProgress().getOrDefault(targetObjective.id(), 0);
            if (currentPreProgress >= targetObjective.amount()) continue;

            final Quest finalQuest = quest;
            final Objective finalTargetObjective = targetObjective;

            ACTIVE_QUESTS.computeIfPresent(questId, (id, state) -> {

                final int currentObjProgress = state.objectiveProgress().getOrDefault(finalTargetObjective.id(), 0);
                final int newProgress = Math.min(currentObjProgress + amount, finalTargetObjective.amount());

                state.objectiveProgress().put(finalTargetObjective.id(), newProgress);
                state.participants().add(playerUUID);

                // Check if all objectives in this quest meet or exceed their required amounts
                boolean allCompleted = finalQuest.objectives().stream()
                        .allMatch(o -> state.objectiveProgress().getOrDefault(o.id(), 0) >= o.amount());

                if (allCompleted) {
                    MessageUtils.broadcast(Messages.PREFIX, Messages.QUEST_COMPLETED);

                    CompletableFuture.runAsync(() -> {
                        plugin.getDatabaseProvider().clearActiveQuestBackup(id);
                        for (final String reward : finalQuest.rewards()) {
                            plugin.getDatabaseProvider().grantRewards(state.participants(), reward);
                        }
                    });

                    return null;
                }

                return new ActiveQuestState(state.objectiveProgress(), state.participants(), state.endTime());
            });
        }
    }
}