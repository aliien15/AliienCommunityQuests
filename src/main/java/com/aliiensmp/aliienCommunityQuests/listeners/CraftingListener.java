package com.aliiensmp.aliienCommunityQuests.listeners;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.enums.ObjectiveType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.CraftItemEvent;

import java.util.Optional;

public class CraftingListener extends AbstractQuestListener {

    public CraftingListener(final AliienCommunityQuests plugin) {
        super(plugin);
    }

    @EventHandler
    public void onCrafting(final CraftItemEvent event) {
        Optional.ofNullable(event.getCurrentItem())
                .ifPresent(item ->
                        handleProgress(
                                event.getWhoClicked().getUniqueId(),
                                ObjectiveType.ITEM_CRAFT,
                                event.getRecipe().getResult().getType().name(),
                                item.getAmount()
                        ));
    }
}
