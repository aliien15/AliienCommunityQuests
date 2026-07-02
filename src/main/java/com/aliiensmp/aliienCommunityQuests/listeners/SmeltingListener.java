package com.aliiensmp.aliienCommunityQuests.listeners;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.enums.ObjectiveType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.FurnaceExtractEvent;

public class SmeltingListener extends AbstractQuestListener {

    public SmeltingListener(final AliienCommunityQuests plugin) {
        super(plugin);
    }

    @EventHandler
    public void onSmelting(final FurnaceExtractEvent event) {
        handleProgress(
                event.getPlayer().getUniqueId(),
                ObjectiveType.SMELTING,
                event.getItemType().name(),
                event.getItemAmount()
        );
    }
}
