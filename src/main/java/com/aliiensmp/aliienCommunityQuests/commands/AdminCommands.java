package com.aliiensmp.aliienCommunityQuests.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.config.Messages;
import com.aliiensmp.aliienCommunityQuests.manager.QuestManager;
import com.aliiensmp.core.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@CommandAlias("communityquests|communityquest|cquest|cquests|cq")
public class AdminCommands extends BaseCommand {

    private final AliienCommunityQuests plugin;

    public AdminCommands(final AliienCommunityQuests plugin) {
        this.plugin = plugin;
    }

    @Subcommand("admin reload")
    @CommandPermission("aliien.communityquests.admin.reload")
    public void onReload(final CommandSender sender) {
        MessageUtils.send(sender, Messages.PREFIX, Messages.RELOAD_RELOADING);

        CompletableFuture.runAsync(() -> {
            boolean success = plugin.loadConfig();

            Runnable task = () -> {
                if (success) {
                    MessageUtils.send(sender, Messages.PREFIX, Messages.RELOAD_SUCCESS);
                } else {
                    MessageUtils.send(sender, Messages.PREFIX, Messages.RELOAD_FAIL);
                }
            };

            if (sender instanceof Player player) {
                player.getScheduler().run(plugin, scheduledTask -> task.run(), null);
            } else {
                plugin.getServer().getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
            }
        });
    }

    @Subcommand("admin reset")
    @CommandPermission("aliien.communityquests.admin.reset")
    @CommandCompletion("@questIds")
    public void onQuestReset(final CommandSender sender, final String questId) {
        if (!QuestManager.ACTIVE_QUESTS.containsKey(questId)) {
            MessageUtils.send(sender, Messages.PREFIX, Messages.QUEST_NOT_FOUND);
            return;
        }

        MessageUtils.broadcast(Messages.PREFIX, Messages.QUEST_RESET);

        QuestManager.ACTIVE_QUESTS.remove(questId);
        CompletableFuture.runAsync(() -> plugin.getDatabaseProvider().clearActiveQuestBackup(questId));

        plugin.getQuestManager().generateMissingQuests();
    }

    @Subcommand("admin resetall")
    @CommandPermission("aliien.communityquests.admin.resetall")
    public void onQuestResetAll(final CommandSender sender) {
        if (QuestManager.ACTIVE_QUESTS.isEmpty()) {
            MessageUtils.send(sender, Messages.PREFIX, Messages.QUEST_NONE_ACTIVE);
            return;
        }

        final List<String> activeIds = List.copyOf(QuestManager.ACTIVE_QUESTS.keySet());

        QuestManager.ACTIVE_QUESTS.clear();
        MessageUtils.broadcast(Messages.PREFIX, Messages.QUEST_RESET_ALL);

        CompletableFuture.runAsync(() -> {
            activeIds.forEach(id -> plugin.getDatabaseProvider().clearActiveQuestBackup(id));
        }).thenRun(() -> plugin.getQuestManager().generateMissingQuests());
    }
}