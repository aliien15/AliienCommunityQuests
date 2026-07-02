package com.aliiensmp.aliienCommunityQuests.listeners;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.enums.ObjectiveType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityBreedEvent;

public class BreedingListener extends AbstractQuestListener {

    public BreedingListener(final AliienCommunityQuests plugin) {
        super(plugin);
    }

    @EventHandler
    public void onBreeding(final EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player player)) return;

        handleProgress(
                player.getUniqueId(),
                ObjectiveType.BREEDING,
                event.getEntity().getType().name()
        );
    }
}
