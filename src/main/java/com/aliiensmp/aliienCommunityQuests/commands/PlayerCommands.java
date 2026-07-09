package com.aliiensmp.aliienCommunityQuests.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.menu.Menu;
import org.bukkit.entity.Player;

@CommandAlias("communityquests|communityquest|cquest|cquests|cq")
public class PlayerCommands extends BaseCommand {

    private final AliienCommunityQuests plugin;

    public PlayerCommands(final AliienCommunityQuests plugin) {
        this.plugin = plugin;
    }

    @Default
    @CommandPermission("aliien.communityquests.menu")
    public void onDefault(final Player player) {
        new Menu(plugin).open(player, 1);
    }
}