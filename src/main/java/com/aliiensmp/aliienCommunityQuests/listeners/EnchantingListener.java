package com.aliiensmp.aliienCommunityQuests.listeners;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.enums.ObjectiveType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.enchantment.EnchantItemEvent;

public class EnchantingListener extends AbstractQuestListener {

    public EnchantingListener(AliienCommunityQuests plugin) {
        super(plugin);
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        handleProgress(
                event.getEnchanter().getUniqueId(),
                ObjectiveType.ENCHANTING,
                event.getItem().getType().name()
        );
    }
}
