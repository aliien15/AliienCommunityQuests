package com.aliiensmp.aliienCommunityQuests.listeners;

import com.aliiensmp.aliienCommunityQuests.AliienCommunityQuests;
import com.aliiensmp.aliienCommunityQuests.config.Settings;
import com.aliiensmp.aliienCommunityQuests.enums.ObjectiveType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener extends AbstractQuestListener {

    public BlockBreakListener(final AliienCommunityQuests plugin) {
        super(plugin);
    }

    @EventHandler
    public void onBlockBreak(final BlockBreakEvent event) {
        final String rawBlockName = event.getBlock().getType().name();

        final String parsedBlockName = (Settings.COUNT_DEEPSLATE_ORES && rawBlockName.startsWith("DEEPSLATE_") && rawBlockName.endsWith("_ORE"))
                ? rawBlockName.replace("DEEPSLATE_", "")
                : rawBlockName;

        handleProgress(
                event.getPlayer().getUniqueId(),
                ObjectiveType.BLOCK_BREAK,
                parsedBlockName
        );
    }
}