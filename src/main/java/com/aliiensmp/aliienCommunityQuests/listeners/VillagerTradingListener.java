package com.aliiensmp.aliienCommunityQuests.listeners;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.enums.ObjectiveType;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.event.EventHandler;

public class VillagerTradingListener extends AbstractQuestListener {

    public VillagerTradingListener(final AliienCommunityQuests plugin) {
        super(plugin);
    }

    @EventHandler
    public void onVillagerTrade(final PlayerTradeEvent event) {
        handleProgress(
                event.getPlayer().getUniqueId(),
                ObjectiveType.VILLAGER_TRADING,
                event.getTrade().getResult().getType().name(),
                event.getTrade().getResult().getAmount()
        );
    }
}
