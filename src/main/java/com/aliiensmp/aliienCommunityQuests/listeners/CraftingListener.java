package com.aliiensmp.aliienCommunityQuests.listeners;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.enums.ObjectiveType;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class CraftingListener extends AbstractQuestListener {

    public CraftingListener(final AliienCommunityQuests plugin) {
        super(plugin);
    }

    @EventHandler
    public void onCrafting(final CraftItemEvent event) {
        Optional.ofNullable(event.getCurrentItem()).ifPresent(item -> {

            int progress = item.getAmount();

            if (event.isShiftClick()) {
                int totalCrafts = Arrays.stream(event.getInventory().getMatrix())
                        .filter(Objects::nonNull)
                        .filter(ingredient -> ingredient.getType() != Material.AIR)
                        .mapToInt(ItemStack::getAmount)
                        .min()
                        .orElse(1);

                progress *= totalCrafts;
            }

            handleProgress(
                    event.getWhoClicked().getUniqueId(),
                    ObjectiveType.ITEM_CRAFT,
                    item.getType().name(),
                    progress
            );
        });
    }
}