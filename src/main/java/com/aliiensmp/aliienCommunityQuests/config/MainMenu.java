package com.aliiensmp.aliienCommunityQuests.config;

import com.aliiensmp.aliienCommunityQuests.config.records.MenuItem;
import com.aliiensmp.aliienCommunityQuests.enums.MenuAction;
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

    /**
     * Loads all the menu items into the {@code ITEMS_LIST} so it can be rapidly used
     * when we have to open the menu for players.
     *
     * @param config the yml config file where we can find the menu items
     */
    public static void load(YamlDocument config) {
        ITEMS_LIST.clear();

        Optional.ofNullable(config.getSection("items")).ifPresent(itemsSection -> {
            itemsSection.getRoutesAsStrings(false).forEach(itemId -> {
                final Section iSec = itemsSection.getSection(itemId);
                if (iSec == null) return;

                // Parse Menu Options
                final MenuAction action = MenuAction.valueOf(iSec.getString("action", "NONE"));
                final Material material = Material.valueOf(iSec.getString("material", "STONE"));
                final List<Integer> slots = iSec.getIntList("slots", List.of());
                final String name = iSec.getString("name");
                final List<String> lore = iSec.getStringList("lore");
                final int customModelData = iSec.getInt("custom-model-data", 0);
                final boolean glow = iSec.getBoolean("glow", false);
                final List<ItemFlag> flags = iSec.getStringList("item-flags").stream()
                        .map(ItemFlag::valueOf)
                        .toList();

                // Construct the final Record and add it to the items list
                ITEMS_LIST.add(new MenuItem(action, material, slots, name, lore, customModelData, glow, flags));
            });
        });
    }
}