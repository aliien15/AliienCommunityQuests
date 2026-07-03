package com.aliiensmp.aliienCommunityQuests.database;

import com.aliiensmp.aliienCommunityQuests.enums.ActiveQuestState;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DatabaseProvider {

    /**
     * Startup method
     */
    void init();

    /**
     * Pushes completed quest rewards to offline/online participants.
     *
     * @param participants The set of unique players who contributed.
     * @param rewardId The configuration key for the reward item.
     * @return A future completing when the batch insert finishes.
     */
    CompletableFuture<Void> grantRewards(final Set<UUID> participants, final String rewardId);

    /**
     * Retrieves all unredeemed rewards for a player when they open the GUI hub.
     *
     * @param playerUuid The UUID of the player checking their stash.
     * @return A list of configuration keys representing their pending rewards.
     */
    CompletableFuture<List<String>> getPendingRewards(final UUID playerUuid);

    /**
     * Wipes a player's pending rewards from the database after a successful claim.
     *
     * @param playerUuid The UUID of the player who claimed their stash.
     * @return True if the deletion was successful, false otherwise.
     */
    CompletableFuture<Boolean> clearPendingRewards(final UUID playerUuid);

    /**
     * Backs up the current server-wide progress on a timed interval.
     *
     * @param questId The active quest ID.
     * @param progress The current global completion integer.
     * @param participants The set of UUIDs currently contributing.
     * @return A future completing when the backup is safely stored.
     */
    CompletableFuture<Void> saveActiveQuest(final String questId, final int progress, final Set<UUID> participants);

    /**
     * Fetches all in-progress quests and their contributors upon server startup.
     *
     * @return A map linking the Quest ID to its saved progress and participants.
     */
    CompletableFuture<Map<String, ActiveQuestState>> loadActiveCache();

    /**
     * Wipes a completed quest from the backup tables so it is not reloaded on reboot.
     *
     * @param questId The ID of the quest that just reached 100%.
     * @return True if the deletion was successful, false otherwise.
     */
    CompletableFuture<Boolean> clearActiveQuestBackup(final String questId);
}
