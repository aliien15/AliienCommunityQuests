package com.aliiensmp.aliienCommunityQuests.config.records;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import java.util.List;

public record Quest(
        String id,
        String duration,
        int objectivesAmount,
        String objectiveFormat,
        int priority,
        String name,
        List<String> lore,
        Material material,
        int customModelData,
        boolean glow,
        List<ItemFlag> itemFlags,
        List<Objective> objectives,
        List<String> rewards
) {
}
