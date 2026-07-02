package com.aliiensmp.aliienCommunityQuests.listeners;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.enums.ObjectiveType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerFishEvent;

public class PlayerFishListener extends AbstractQuestListener {

    public PlayerFishListener(final AliienCommunityQuests plugin) {
        super(plugin);
    }

    @EventHandler
    public void onPlayerFish(final PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        if (event.getCaught() instanceof Item caughtItem) {
            handleProgress(
                    event.getPlayer().getUniqueId(),
                    ObjectiveType.PLAYER_FISH,
                    caughtItem.getItemStack().getType().name()
            );
        }
    }
}
