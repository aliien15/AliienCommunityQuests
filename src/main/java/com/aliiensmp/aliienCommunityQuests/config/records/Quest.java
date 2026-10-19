package com.aliiensmp.aliienCommunityQuests.config.records;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.checkerframework.checker.units.qual.N;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record Quest(
        @NotNull String id,
        @NotNull String duration,
        int objectivesAmount,
        @NotNull String objectiveFormat,
        @NotNull String name,
        @NotNull List<String> lore,
        @NotNull Material material,
        int customModelData,
        boolean glow,
        @NotNull List<ItemFlag> itemFlags,
        @NotNull List<Objective> objectives,
        @NotNull List<String> rewards,
        @NotNull List<String> topRewards
) {
}
