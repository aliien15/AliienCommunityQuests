package com.aliiensmp.aliienCommunityQuests.menu;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.config.*;
import com.aliiensmp.aliienCommunityQuests.config.records.ActiveQuestState;
import com.aliiensmp.aliienCommunityQuests.config.records.MenuItem;
import com.aliiensmp.aliienCommunityQuests.config.records.Objective;
import com.aliiensmp.aliienCommunityQuests.config.records.Quest;
import com.aliiensmp.aliienCommunityQuests.enums.MenuAction;
import com.aliiensmp.aliienCommunityQuests.manager.QuestManager;
import com.aliiensmp.core.items.ItemBuilder;
import com.aliiensmp.core.menu.AliienGUI;
import com.aliiensmp.core.menu.ClickableItem;
import com.aliiensmp.core.utils.DurationUtils;
import com.aliiensmp.core.utils.MessageUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

public class Menu {

    private final AliienCommunityQuests plugin;
    private record ActiveQuestEntry(Quest quest, ActiveQuestState state) {}

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
        plugin.getDatabaseProvider()
                .getPendingRewards(player.getUniqueId())
                .thenAccept(rewards -> player.getScheduler().run(plugin, task -> {

                    final int pendingCount = rewards.size();
                    final AliienGUI menu = new AliienGUI(MainMenu.TITLE, MainMenu.ROWS);
                    final List<Integer> questSlots = new ArrayList<>(MainMenu.QUEST_SLOTS.stream().distinct().toList());
                    menu.filterInvalidSlots(questSlots);

                    final List<ActiveQuestEntry> activeEntries = getActiveQuestEntries();
                    final int maxPages = menu.getTotalPages(questSlots.size(), activeEntries.size());
                    final int page = menu.sanitizePage(requestedPage, maxPages);

                    buildStaticLayout(menu, player, page, maxPages, pendingCount);
                    menu.setItems(questSlots, activeEntries, page, entry -> createQuestItem(entry.quest(), entry.state(), player));

                    menu.open(player, page);

                }, null)).exceptionally(ex -> {
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
     * @param maxPages The total number of pages.
     * @param pendingCount the amount of rewards that the player is yet to claim.
     * @requires MainMenu.ITEMS_LIST is successfully loaded in memory and not empty.
     * @ensures Static items are placed into their correct GUI slots with active, routed click handlers.
     */
    private void buildStaticLayout(final AliienGUI menu, final Player player, final int page, final int maxPages, final int pendingCount) {
        MainMenu.ITEMS_LIST.forEach(item -> {
            if (item.action() == MenuAction.PREVIOUS_PAGE && page <= 1) return;
            if (item.action() == MenuAction.NEXT_PAGE && page >= maxPages) return;

            final String targetPageStr = switch (item.action()) {
                case NEXT_PAGE -> String.valueOf(page + 1);
                case PREVIOUS_PAGE -> String.valueOf(page - 1);
                default -> String.valueOf(page);
            };
            final String rewardsStr = String.valueOf(pendingCount);

            final String parsedName = item.name()
                    .replace("%target_page%", targetPageStr)
                    .replace("%pending_rewards%", rewardsStr);

            final List<String> parsedLore = item.lore().stream()
                    .map(line -> line
                            .replace("%target_page%", targetPageStr)
                            .replace("%pending_rewards%", rewardsStr)
                    )
                    .toList();

            final ClickableItem menuItem = new ItemBuilder(item.material())
                    .name(parsedName)
                    .stringLore(parsedLore)
                    .glow(item.glow())
                    .addFlags(item.itemFlags().toArray(new ItemFlag[0]))
                    .customModelData(item.customModelData())
                    .buildClickable(event -> handleClickEvent(item, player, page));

            menu.setItem(item.slots(), menuItem);
        });
    }

    /**
     * Collects the active quest entries in the order defined in quests.yml without building ItemStacks upfront.
     *
     * @return The ordered quest entries for AliienGUI to lazily paginate.
     */
    private List<ActiveQuestEntry> getActiveQuestEntries() {
        return Quests.QUEST_LIST.stream()
                .flatMap(quest -> Optional.ofNullable(QuestManager.ACTIVE_QUESTS.get(quest.id()))
                        .map(state -> new ActiveQuestEntry(quest, state))
                        .stream())
                .toList();
    }

    /**
     * Generates the dynamic visual item for a quest. This acts as the lore parser,
     * safely replacing real-time progress placeholders and expanding list variables natively.
     *
     * @param questData The static configuration record holding the blueprint of the quest.
     * @param state     The active database state holding the real-time progress of the quest.
     * @param player    The player opening the menu
     * @return A completely constructed, localized item ready to be displayed in a GUI.
     */
    private ClickableItem createQuestItem(final Quest questData, final ActiveQuestState state, final Player player) {

        // Calculate the remaining time
        final long timeRemainingMillis = Math.max(0, state.endTime() - System.currentTimeMillis());
        final String formattedTime = DurationUtils.format(
                Duration.ofMillis(timeRemainingMillis),
                Settings.toStyle()
        );

        // Check if all objectives in this quest meet or exceed their required amounts
        final boolean isCompleted = questData.objectives().stream()
                .filter(o -> state.objectiveProgress().containsKey(o.id()))
                .allMatch(o -> state.objectiveProgress().getOrDefault(o.id(), 0) >= o.amount());

        final int playerContribution = state.participantsWithContributions()
                .getOrDefault(player.getUniqueId(), 0);
        final int totalQuestRequirement = questData.objectives().stream()
                .filter(o -> state.objectiveProgress().containsKey(o.id()))
                .mapToInt(Objective::amount)
                .sum();
        final double contributionPercentage = totalQuestRequirement > 0
                ? ((double) playerContribution / totalQuestRequirement) * 100.0
                : 0.0;

        final String formattedContribution = String.format(Locale.US, "%.2f", contributionPercentage);
        final String statusString = isCompleted ? Settings.STATUS_COMPLETED : Settings.STATUS_IN_PROGRESS;

        final List<String> parsedLore = questData.lore().stream()
                .map(line -> line
                        .replace("%time_left%", formattedTime)
                        .replace("%contribution%", formattedContribution)
                        .replace("%status%", statusString)
                )
                .flatMap(line -> {
                    // Handle the expanding list placeholder
                    if (line.contains("%active_objectives%")) {
                        return questData.objectives().stream()
                                .filter(objective -> state.objectiveProgress().containsKey(objective.id()))
                                .map(objective ->
                                        questData.objectiveFormat()
                                                .replace("%target%", Formatting.formatObjective(objective))
                                                .replace("%current%", String.valueOf(state.objectiveProgress().get(objective.id())))
                                                .replace("%amount%", String.valueOf(objective.amount()))
                                );
                    }

                    // Handle the regular ID-based placeholders
                    String parsedLine = line;
                    for (final Objective obj : questData.objectives()) {
                        if (state.objectiveProgress().containsKey(obj.id())) {
                            parsedLine = parsedLine
                                    .replace("%target_" + obj.id() + "%", Formatting.formatObjective(obj))
                                    .replace("%current_" + obj.id() + "%", String.valueOf(state.objectiveProgress().get(obj.id())))
                                    .replace("%amount_" + obj.id() + "%", String.valueOf(obj.amount()));
                        }
                    }

                    return Stream.of(parsedLine);
                })
                .toList();

        return new ItemBuilder(questData.material())
                .name(questData.name()
                        .replace("%time_left%", formattedTime)
                        .replace("%status%", statusString)
                        .replace("%contribution%", formattedContribution)
                )
                .addFlags(questData.itemFlags().toArray(new ItemFlag[0]))
                .glow(questData.glow())
                .stringLore(parsedLore)
                .customModelData(questData.customModelData())
                .buildClickable(event -> {});
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
            case REWARDS -> handleRewards(player, page);
            case NONE -> {}
        }
    }

    /**
     * Handles giving out the quests rewards missing for the player
     *
     * @param player player clicking the item
     */
    private void handleRewards(final Player player, final int page) {
        plugin.getDatabaseProvider().getPendingRewards(player.getUniqueId()).thenAccept(rewardCmds -> player.getScheduler().run(plugin, task -> {

            if (rewardCmds.isEmpty()) {
                MessageUtils.send(player, Messages.PREFIX, Messages.REWARDS_NOT_FOUND);
                return;
            }

            rewardCmds.forEach(rewardCmd -> {
                final String finalCmd = rewardCmd.replace("%player%", player.getName());
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), finalCmd);
            });

            MessageUtils.send(player, Messages.PREFIX, Messages.REWARDS_CLAIMED);
            plugin.getDatabaseProvider().clearPendingRewards(player.getUniqueId())
                    .thenRun(() -> open(player, page));
        }, null)).exceptionally(ex -> {
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
        open(player, currentPage + 1);
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