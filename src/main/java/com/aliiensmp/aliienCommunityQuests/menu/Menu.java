package com.aliiensmp.aliienCommunityQuests.menu;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.config.MainMenu;
import com.aliiensmp.aliienCommunityQuests.config.Messages;
import com.aliiensmp.aliienCommunityQuests.config.Quests;
import com.aliiensmp.aliienCommunityQuests.config.records.MenuItem;
import com.aliiensmp.aliienCommunityQuests.config.records.Objective;
import com.aliiensmp.aliienCommunityQuests.config.records.Quest;
import com.aliiensmp.aliienCommunityQuests.config.records.ActiveQuestState;
import com.aliiensmp.aliienCommunityQuests.enums.MenuAction;
import com.aliiensmp.aliienCommunityQuests.manager.QuestManager;
import com.aliiensmp.core.items.ItemBuilder;
import com.aliiensmp.core.menu.AliienGUI;
import com.aliiensmp.core.menu.ClickableItem;
import com.aliiensmp.core.utils.MessageUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class Menu {

    private final AliienCommunityQuests plugin;

    public Menu(final AliienCommunityQuests plugin) {
        this.plugin = plugin;
    }

    /**
     * The main method for opening the Community Quests menu.
     * Initializes the GUI instance, sends the layout building to helper methods,
     * and opens the inventory for the player.
     *
     * @param player The player who will view the menu.
     * @param requestedPage The specific page number the player is attempting to open.
     * @requires player is not null and is currently online.
     * @ensures An AliienGUI is safely opened for the player on a mathematically valid page (minimum 1).
     */
    public void open(final Player player, final int requestedPage) {
        final int page = parsePage(requestedPage);

        plugin.getDatabaseProvider().getPendingRewards(player.getUniqueId()).thenAccept(rewards -> {
            player.getScheduler().run(plugin, task -> {

                final int pendingCount = rewards.size();
                final String parsedTitle = MainMenu.TITLE.replace("%page%", String.valueOf(page));
                final AliienGUI menu = new AliienGUI(parsedTitle, MainMenu.ROWS);

                buildStaticLayout(menu, player, page, pendingCount);
                buildActiveQuests(menu, page);

                menu.open(player, page);

            }, null);

        }).exceptionally(ex -> {
            plugin.getLogger().severe("Failed to load GUI data for " + player.getName() + ": " + ex.getMessage());
            return null;
        });
    }

    /**
     * Iterates through the static configuration list and paints the background layout.
     * This includes aesthetic items (like glass panes) and clickable navigation buttons.
     *
     * @param menu The active AliienGUI instance being constructed.
     * @param player The player viewing the menu (captured to pass into click events).
     * @param page The current page number (captured to pass into pagination math).
     * @param pendingCount the amount of rewards that the player is yet to claim.
     * @requires MainMenu.ITEMS_LIST is successfully loaded in memory and not empty.
     * @ensures Static items are placed into their correct GUI slots with active, routed click handlers.
     */
    private void buildStaticLayout(AliienGUI menu, Player player, int page, int pendingCount) {
        // Calculate max pages based on active quests
        final int totalQuests = QuestManager.ACTIVE_QUESTS.size();
        final int slotsPerPage = MainMenu.QUEST_SLOTS.size();
        int maxPages = (int) Math.ceil((double) totalQuests / slotsPerPage);
        if (maxPages == 0) maxPages = 1;

        final int finalMaxPages = maxPages;
        MainMenu.ITEMS_LIST.forEach(item -> {
            // Hide pagination arrows if they are a dead end
            if (item.action() == MenuAction.PREVIOUS_PAGE && page <= 1) return;
            if (item.action() == MenuAction.NEXT_PAGE && page >= finalMaxPages) return;

            // Calculate %target_page%
            int targetPage = page;
            if (item.action() == MenuAction.NEXT_PAGE) {
                targetPage = page + 1;
            } else if (item.action() == MenuAction.PREVIOUS_PAGE) {
                targetPage = parsePage(page - 1);
            }

            final String targetStr = String.valueOf(targetPage);
            final String rewardsStr = String.valueOf(pendingCount);

            // Parse the name
            String parsedName = item.name()
                    .replace("%target_page%", targetStr)
                    .replace("%pending_rewards%", rewardsStr);

            // Parse the lore
            List<String> parsedLore = item.lore().stream()
                    .map(line -> line
                            .replace("%target_page%", targetStr)
                            .replace("%pending_rewards%", rewardsStr)
                    )
                    .toList();

            // Build the item
            ItemStack menuItem = new ItemBuilder(item.material())
                    .name(parsedName)
                    .stringLore(parsedLore)
                    .glow(item.glow())
                    .addFlags(item.itemFlags().toArray(new ItemFlag[0]))
                    .customModelData(item.customModelData())
                    .build();

            item.slots().forEach(slot -> menu.setItem(slot, ClickableItem.of(menuItem, event ->
                    handleClickEvent(item, player, page)
            )));
        });
    }

    /**
     * Slices the global active quests cache based on the requested page and maps 
     * the mathematically appropriate quests into the designated empty GUI slots.
     *
     * @param menu The active AliienGUI instance being constructed.
     * @param page The mathematically validated page number to display.
     * @requires MainMenu.QUEST_SLOTS is defined in the config and contains valid integer slot numbers.
     */
    private void buildActiveQuests(final AliienGUI menu, final int page) {
        final int questSlotsPerPage = MainMenu.QUEST_SLOTS.size();
        final int skipAmount = (page - 1) * questSlotsPerPage;

        List<Map.Entry<String, ActiveQuestState>> pagedQuests = QuestManager.ACTIVE_QUESTS.entrySet().stream()
                // Sort quests by their config priority (highest number first)
                .sorted((entry1, entry2) -> {
                    Quest q1 = Quests.QUEST_LIST.stream().filter(q -> q.id().equals(entry1.getKey())).findFirst().orElse(null);
                    Quest q2 = Quests.QUEST_LIST.stream().filter(q -> q.id().equals(entry2.getKey())).findFirst().orElse(null);

                    int priority1 = (q1 != null) ? q1.priority() : 0;
                    int priority2 = (q2 != null) ? q2.priority() : 0;

                    return Integer.compare(priority2, priority1);
                })
                .skip(skipAmount)
                .limit(questSlotsPerPage)
                .toList();

        for (int idx = 0; idx < pagedQuests.size(); idx++) {
            final int slot = MainMenu.QUEST_SLOTS.get(idx);
            final Map.Entry<String, ActiveQuestState> entry = pagedQuests.get(idx);

            final Quest questData = Quests.QUEST_LIST.stream()
                    .filter(q -> q.id().equals(entry.getKey()))
                    .findFirst()
                    .orElse(null);

            if (questData == null) continue;

            ItemStack questItem = createQuestItem(questData, entry.getValue());
            menu.setItem(slot, ClickableItem.empty(questItem));
        }
    }

    /**
     * Generates the dynamic visual ItemStack for a quest. This acts as the functional lore parser,
     * safely replacing real-time progress placeholders and expanding list variables natively.
     *
     * @param questData The static configuration record holding the blueprint of the quest.
     * @param state     The active database state holding the real-time progress of the quest.
     * @return A completely constructed, localized ItemStack ready to be displayed in a GUI.
     */
    private ItemStack createQuestItem(Quest questData, ActiveQuestState state) {
        final List<String> parsedLore = questData.lore().stream()
                .flatMap(line -> {
                    // Handle the expanding list placeholder
                    if (line.contains("%active_objectives")) {
                        return questData.objectives().stream()
                                .filter(objective -> state.objectiveProgress().containsKey(objective.id()))
                                .map(objective ->
                                        questData.objectiveFormat()
                                                .replace("%target%", objective.target())
                                                .replace("%current%", String.valueOf(state.objectiveProgress().get(objective.id())))
                                                .replace("%amount%", String.valueOf(objective.amount()))
                                );
                    }

                    // Handle the regular ID-based placeholders
                    String parsedLine = line;
                    for (final Objective obj : questData.objectives()) {
                        if (state.objectiveProgress().containsKey(obj.id())) {
                            parsedLine = parsedLine
                                    .replace("%current_" + obj.id() + "%", String.valueOf(state.objectiveProgress().get(obj.id())))
                                    .replace("%amount_" + obj.id() + "%", String.valueOf(obj.amount()));
                        }
                    }

                    return Stream.of(parsedLine);
                })
                .toList();

        return new ItemBuilder(questData.material())
                .name(questData.name())
                .addFlags(questData.itemFlags().toArray(new ItemFlag[0]))
                .glow(questData.glow())
                .stringLore(parsedLore)
                .customModelData(questData.customModelData())
                .build();
    }

    /**
     * @param requestedPage the page that the player wants to open the menu on
     * @return the page converted to a valid number (in case the number is invalid,
     * so if it is less than zero and more than the max amount of pages)
     */
    private int parsePage(final int requestedPage) {
        return Math.max(1, requestedPage);
    }

    /**
     * Handled the clicking item event depending on what the item action is
     *
     * @param item item to put an event on
     * @param player player clicking the item
     */
    private void handleClickEvent(final MenuItem item, final Player player, final int page) {
        switch (item.action()) {
            case NEXT_PAGE -> handleNextPage(player, page);
            case PREVIOUS_PAGE -> handlePreviousPage(player, page);
            case REWARDS -> handleRewards(item, player);
            case NONE -> {}
        }
    }

    /**
     * Handles giving out the quests rewards missing for the player
     *
     * @param player player clicking the item
     * @param item the menu item
     */
    private void handleRewards(final MenuItem item, final Player player) {
        plugin.getDatabaseProvider().getPendingRewards(player.getUniqueId()).thenAccept(rewardCmds -> {
            player.getScheduler().run(plugin, task -> {

                // Check if they even have rewards
                if (rewardCmds.isEmpty()) {
                    MessageUtils.send(player, Messages.PREFIX, Messages.REWARDS_NOT_FOUND);
                    return;
                }

                // Dispatch the reward commands
                rewardCmds.forEach(rewardCmd -> {
                    final String finalCmd = rewardCmd.replace("%player%", player.getName());
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), finalCmd);
                });

                MessageUtils.send(player, Messages.PREFIX, Messages.REWARDS_CLAIMED);
                CompletableFuture.runAsync(() -> plugin.getDatabaseProvider().clearPendingRewards(player.getUniqueId()));
            }, null);

        }).exceptionally(ex -> {
            plugin.getLogger().severe("Failed to load rewards for " + player.getName() + ": " + ex.getMessage());
            return null;
        });
    }

    /**
     * Handles the next page pagination event
     *
     * @param player player clicking the item
     * @param currentPage the page the player is currently viewing
     */
    private void handleNextPage(final Player player, final int currentPage) {
        final int totalQuests = QuestManager.ACTIVE_QUESTS.size();
        final int slotsPerPage = MainMenu.QUEST_SLOTS.size();

        int maxPages =  (int) Math.ceil((double) totalQuests / slotsPerPage);

        // If there are 0 active quests
        if (maxPages == 0) maxPages = 1;

        if (currentPage < maxPages) {
            open(player, currentPage + 1);
        }
    }

    /**
     * Handles the previous page pagination event
     *
     * @param player player clicking the item
     * @param currentPage the page the player is currently viewing
     */
    private void handlePreviousPage(final Player player, final int currentPage) {
        if (currentPage > 1) open(player, currentPage - 1);
    }
}
