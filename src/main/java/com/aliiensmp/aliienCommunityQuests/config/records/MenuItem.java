package com.aliiensmp.aliienCommunityQuests.config.records;

import com.aliiensmp.aliienCommunityQuests.menu.MenuAction;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import java.util.List;

public record MenuItem(
        MenuAction action,
        Material material,
        List<Integer> slots,
        String name,
        List<String> lore,
        int customModelData,
        boolean glow,
        List<ItemFlag> itemFlags
) {
}
