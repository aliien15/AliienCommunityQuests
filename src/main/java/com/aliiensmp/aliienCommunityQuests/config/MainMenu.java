package com.aliiensmp.aliienCommunityQuests.config;

import com.aliiensmp.aliienCommunityQuests.config.records.MenuItem;
import com.aliiensmp.aliienCommunityQuests.menu.MenuAction;
import com.aliiensmp.core.config.Key;
import com.aliiensmp.core.lib.boostedyaml.YamlDocument;
import com.aliiensmp.core.lib.boostedyaml.block.implementation.Section;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainMenu {

    @Key("menu.title")
    public static final String TITLE = "&8Quests - Page %page%";

    @Key("menu.rows")
    public static final int ROWS = 3;

    @Key("layout.quest-slots")
    public static final List<Integer> QUEST_SLOTS = List.of(10, 11, 12, 13, 14, 15, 16);
    
    public static final List<MenuItem> ITEMS_LIST = new ArrayList<>();

    public static void load(YamlDocument config) {
        ITEMS_LIST.clear();

        Optional.ofNullable(config.getSection("items")).ifPresent(itemsSection -> {
            itemsSection.getRoutesAsStrings(false).forEach(itemId -> {
                Section iSec = itemsSection.getSection(itemId);
                if (iSec == null) return;

                // Parse Menu Options
                MenuAction action = MenuAction.valueOf(iSec.getString("action", "NONE"));
                Material material = Material.valueOf(iSec.getString("material", "STONE"));
                List<Integer> slots = iSec.getIntList("slots", List.of());
                String name = iSec.getString("name");
                List<String> lore = iSec.getStringList("lore");
                int customModelData = iSec.getInt("custom-model-data", 0);
                boolean glow = iSec.getBoolean("glow", false);
                List<ItemFlag> flags = iSec.getStringList("item-flags").stream()
                        .map(ItemFlag::valueOf)
                        .toList();

                // Construct the final Record and add it to the items list
                ITEMS_LIST.add(new MenuItem(action, material, slots, name, lore, customModelData, glow, flags));
            });
        });
    }
}