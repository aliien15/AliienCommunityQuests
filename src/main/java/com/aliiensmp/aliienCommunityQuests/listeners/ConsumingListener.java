package com.aliiensmp.aliienCommunityQuests.listeners;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.enums.ObjectiveType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class ConsumingListener extends AbstractQuestListener {

    public ConsumingListener(final AliienCommunityQuests plugin) {
        super(plugin);
    }

    @EventHandler
    public void onConsuming(final PlayerItemConsumeEvent event) {
        handleProgress(
                event.getPlayer().getUniqueId(),
                ObjectiveType.CONSUMING,
                event.getItem().getType().name()
        );
    }
}
