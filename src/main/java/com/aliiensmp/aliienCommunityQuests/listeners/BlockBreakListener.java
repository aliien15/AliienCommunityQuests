package com.aliiensmp.aliienCommunityQuests.listeners;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.enums.ObjectiveType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener extends AbstractQuestListener {

    public BlockBreakListener(AliienCommunityQuests plugin) {
        super(plugin);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        handleProgress(
                event.getPlayer().getUniqueId(),
                ObjectiveType.BLOCK_BREAK,
                event.getBlock().getType().name()
        );
    }
}