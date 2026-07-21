package com.aliiensmp.aliienCommunityQuests.config;

import com.aliiensmp.aliienCommunityQuests.config.records.Objective;
import com.aliiensmp.aliienCommunityQuests.config.records.Quest;
import com.aliiensmp.aliienCommunityQuests.enums.ObjectiveType;
import com.aliiensmp.core.lib.boostedyaml.YamlDocument;
import com.aliiensmp.core.lib.boostedyaml.block.implementation.Section;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Quests {

    public static final @NotNull List<Quest> QUEST_LIST = new ArrayList<>();

    public static void load(YamlDocument config) {
        QUEST_LIST.clear();

        Optional.ofNullable(config.getSection("quests")).ifPresent(questsSection -> {
            questsSection.getRoutesAsStrings(false).forEach(questId -> {
                final Section qSec = questsSection.getSection(questId);
                if (qSec == null) return;

                // Parse Settings
                final String duration = qSec.getString("settings.duration");
                final int objAmount = qSec.getInt("settings.objectives-amount");
                final String objFormat = qSec.getString("settings.objective-format");
                final int priority = qSec.getInt("settings.priority", 99);

                // Parse Menu Options
                final String name = qSec.getString("menu-options.name");
                final List<String> lore = qSec.getStringList("menu-options.lore");
                final Material material = Material.valueOf(qSec.getString("menu-options.material", "STONE"));
                final int customModelData = qSec.getInt("menu-options.custom-model-data", 0);
                final boolean glow = qSec.getBoolean("menu-options.glow", false);
                final List<ItemFlag> flags = qSec.getStringList("menu-options.item-flags").stream()
                        .map(ItemFlag::valueOf)
                        .toList();

                // Parse Objectives
                final List<Objective> objectives = new ArrayList<>();
                Optional.ofNullable(qSec.getSection("objectives")).ifPresent(objSec -> {
                    objSec.getRoutesAsStrings(false).forEach(objId -> {
                        objectives.add(new Objective(
                                objId,
                                ObjectiveType.valueOf(objSec.getString(objId + ".type")),
                                objSec.getString(objId + ".target"),
                                objSec.getInt(objId + ".amount")
                        ));
                    });
                });

                // Parse Rewards
                final List<String> rewards = qSec.getStringList("rewards");

                // Construct the final Record and add it to the quests list
                QUEST_LIST.add(new Quest(questId, duration, objAmount, objFormat, priority, name, lore, material, customModelData, glow, flags, objectives, rewards));
            });
        });
    }
}