package com.aliiensmp.aliienCommunityQuests.listeners;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.config.Messages;
import com.aliiensmp.aliienCommunityQuests.config.Quests;
import com.aliiensmp.aliienCommunityQuests.enums.ActiveQuestState;
import com.aliiensmp.aliienCommunityQuests.enums.ObjectiveType;
import com.aliiensmp.core.utils.MessageUtils;
import org.bukkit.event.Listener;

import java.util.UUID;

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
        ACTIVE_QUESTS.forEach((questId, state) -> {
            Quests.QUEST_LIST.stream()
                    .filter(q -> q.id().equals(questId))
                    .findFirst()
                    .ifPresent(questConfig -> {
                        questConfig.objectives().stream()
                                .filter(obj -> obj.type() == type)
                                .filter(obj -> obj.target().equals(target))
                                .findFirst()
                                .ifPresent(objective -> {

                                    final int newProgress = state.progress() + amount;
                                    state.participants().add(playerUUID);

                                    if (newProgress >= objective.amount()) {
                                        ACTIVE_QUESTS.remove(questId);
                                        plugin.getDatabaseProvider().clearActiveQuestBackup(questId);
                                        questConfig.rewards().forEach(reward -> {
                                            plugin.getDatabaseProvider().grantRewards(state.participants(), reward);
                                        });
                                        MessageUtils.broadcast(Messages.PREFIX, Messages.QUEST_COMPLETED);
                                    } else {
                                        ACTIVE_QUESTS.put(questId, new ActiveQuestState(newProgress, state.participants()));
                                    }
                                });
                    });
        });
    }
}