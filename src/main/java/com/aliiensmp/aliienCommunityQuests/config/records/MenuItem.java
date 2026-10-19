package com.aliiensmp.aliienCommunityQuests.config.records;

import com.aliiensmp.aliienCommunityQuests.enums.MenuAction;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record MenuItem(
        @NotNull MenuAction action,
        @NotNull Material material,
        @NotNull List<Integer> slots,
        String name,
        @NotNull List<String> lore,
        int customModelData,
        boolean glow,
        @NotNull List<ItemFlag> itemFlags
) {
}
