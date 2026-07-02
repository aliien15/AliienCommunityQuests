package com.aliiensmp.aliienCommunityQuests.listeners;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.enums.ObjectiveType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityTameEvent;

public class TamingListener extends AbstractQuestListener {

    public TamingListener(final AliienCommunityQuests plugin) {
        super(plugin);
    }

    @EventHandler
    public void onTaming(final EntityTameEvent event) {
        handleProgress(
                event.getOwner().getUniqueId(),
                ObjectiveType.TAMING,
                event.getEntityType().name()
        );
    }
}
