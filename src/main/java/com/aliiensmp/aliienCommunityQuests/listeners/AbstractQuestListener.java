package com.aliiensmp.aliienCommunityQuests.listeners;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.config.Messages;
import com.aliiensmp.aliienCommunityQuests.config.Quests;
import com.aliiensmp.aliienCommunityQuests.config.records.Quest;
import com.aliiensmp.aliienCommunityQuests.config.records.ActiveQuestState;
import com.aliiensmp.aliienCommunityQuests.enums.ObjectiveType;
import com.aliiensmp.aliienCommunityQuests.manager.QuestManager;
import com.aliiensmp.core.utils.MessageUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.aliiensmp.aliienCommunityQuests.manager.QuestManager.ACTIVE_QUESTS;

public abstract class AbstractQuestListener implements Listener {

    protected final AliienCommunityQuests plugin;

    /**
     * Constructs a new abstract quest listener.
     *
     * @param plugin The main plugin instance.
     * @requires plugin != null
     * @ensures this.plugin is initialized
     */
    public AbstractQuestListener(final AliienCommunityQuests plugin) {
        this.plugin = plugin;
    }

    /**
     * Processes progress for any active community quest by incrementing the progress by 1 unit.
     *
     * @param playerUUID The UUID of the player contributing.
     * @param type       The type of objective being completed.
     * @param target     The target string (e.g., Block name, Entity name).
     * @requires playerUUID != null && type != null && target != null && !target.isEmpty()
     * @ensures The quest progress will be incremented by 1 if the target matches an active objective.
     */
    protected void handleProgress(final UUID playerUUID, final ObjectiveType type, final String target) {
        handleProgress(playerUUID, type, target, 1);
    }

    /**
     * Processes progress for any active community quest by a specified amount.
     *
     * @param playerUUID The UUID of the player contributing.
     * @param type       The type of objective being completed.
     * @param target     The target string (e.g., Block name, Entity name).
     * @param amount     The amount to increment the current progress by.
     * @requires playerUUID != null && type != null && target != null && !target.isEmpty() && amount > 0
     * @ensures The objective progress in ACTIVE_QUESTS is incremented by the amount (capped at max requirement),
     *          the player is added to the participants list, and completion logic is triggered if the quest finishes.
     */
    protected void handleProgress(final UUID playerUUID, final ObjectiveType type, final String target, final int amount) {
        Map<String, List<QuestManager.ActiveObjectiveContext>> typeMap = QuestManager.LISTENER_CACHE.get(type);
        if (typeMap == null || typeMap.isEmpty()) return;

        List<QuestManager.ActiveObjectiveContext> contexts = typeMap.get(target.toUpperCase(Locale.ROOT));
        if (contexts == null || contexts.isEmpty()) return;

        final List<CompletableFuture<Void>> completionFutures = new ArrayList<>();

        for (QuestManager.ActiveObjectiveContext context : contexts) {
            ACTIVE_QUESTS.computeIfPresent(context.questId(), (id, state) -> {

                int currentProgress = state.objectiveProgress().getOrDefault(context.objectiveId(), 0);
                if (currentProgress >= context.requiredAmount()) return state;

                int newProgress = Math.min(currentProgress + amount, context.requiredAmount());
                state.objectiveProgress().put(context.objectiveId(), newProgress);
                state.participants().add(playerUUID);

                if (isQuestCompleted(id, state)) {
                    completionFutures.add(handleQuestCompletion(id, state));
                    return null;
                }

                return state;
            });
        }

        if (!completionFutures.isEmpty()) {
            plugin.getQuestManager().rebuildListenerCache();
            CompletableFuture.allOf(completionFutures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> plugin.getServer().getGlobalRegionScheduler().run(plugin, task ->
                            plugin.getQuestManager().generateMissingQuests()
                    ));
        }
    }

    /**
     * Helper method to check completion against the selected objectives from the quest blueprint.
     *
     * @param questId The ID of the quest to look up.
     * @param state   The active state containing selected objectives and progress.
     * @return true if all selected objectives are complete.
     */
    private boolean isQuestCompleted(String questId, ActiveQuestState state) {
        return Quests.QUEST_LIST.stream()
                .filter(quest -> quest.id().equals(questId))
                .findFirst()
                .map(quest -> quest.objectives().stream()
                        .filter(objective -> state.objectiveProgress().containsKey(objective.id()))
                        .allMatch(objective -> state.objectiveProgress().getOrDefault(objective.id(), 0) >= objective.amount()))
                .orElse(false);
    }

    /**
     * Isolates the completion logic to keep the hot path clean, handling database saves and rewards.
     *
     * @param questId The ID of the completed quest.
     * @param state   The active state containing the progress and participants.
     * @requires questId != null && !questId.isEmpty() && state != null && !state.participants().isEmpty()
     * @ensures A completion broadcast is sent, the quest is wiped from the database backup,
     *          and rewards are queued for distribution (either globally or into the offline stash).
     */
    private CompletableFuture<Void> handleQuestCompletion(String questId, ActiveQuestState state) {
        MessageUtils.broadcast(Messages.PREFIX, Messages.QUEST_COMPLETED);

        Quest completedQuest = null;
        for (Quest quest : Quests.QUEST_LIST) {
            if (!quest.id().equals(questId)) continue;
            completedQuest = quest;
            break;
        }

        if (completedQuest == null) return CompletableFuture.completedFuture(null);
        final Quest finalQuest = completedQuest;

        return plugin.getDatabaseProvider().clearActiveQuestBackup(questId).thenRun(() ->
                plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
                    for (UUID uuid : state.participants()) {
                        final Player player = plugin.getServer().getPlayer(uuid);

                        // Handle offline players
                        if (player == null || !player.isOnline()) {
                            for (String reward : finalQuest.rewards()) {
                                plugin.getDatabaseProvider().grantRewards(Collections.singleton(uuid), reward);
                            }
                            continue;
                        }

                        // Handle online players
                        for (String reward : finalQuest.rewards()) {
                            final String cmd = reward.replace("%player%", player.getName());
                            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
                        }
                    }
                }));
    }
}