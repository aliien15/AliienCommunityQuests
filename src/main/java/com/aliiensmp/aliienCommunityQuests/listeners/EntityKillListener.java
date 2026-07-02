package com.aliiensmp.aliienCommunityQuests.listeners;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.enums.ObjectiveType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Optional;

public class EntityKillListener extends AbstractQuestListener {

    public EntityKillListener(AliienCommunityQuests plugin) {
        super(plugin);
    }

    @EventHandler
    public void onEntityKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        Optional.ofNullable(killer)
                .ifPresent(player -> handleProgress(
                        player.getUniqueId(),
                        ObjectiveType.ENTITY_KILL,
                        event.getEntity().getType().name()
                ));
    }
}
