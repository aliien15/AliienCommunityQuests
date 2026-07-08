package com.aliiensmp.aliienCommunityQuests;

import com.aliiensmp.aliienCommunityQuests.config.MainMenu;
import com.aliiensmp.aliienCommunityQuests.config.Messages;
import com.aliiensmp.aliienCommunityQuests.config.Quests;
import com.aliiensmp.aliienCommunityQuests.config.Settings;
import com.aliiensmp.aliienCommunityQuests.database.DatabaseProvider;
import com.aliiensmp.aliienCommunityQuests.database.options.H2;
import com.aliiensmp.aliienCommunityQuests.database.options.MariaDB;
import com.aliiensmp.aliienCommunityQuests.database.options.MySQL;
import com.aliiensmp.aliienCommunityQuests.database.options.SQLite;
import com.aliiensmp.aliienCommunityQuests.listeners.*;
import com.aliiensmp.aliienCommunityQuests.manager.QuestManager;
import com.aliiensmp.core.AliienCore;
import com.aliiensmp.core.config.ConfigManager;
import com.aliiensmp.core.lib.boostedyaml.YamlDocument;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;

public final class AliienCommunityQuests extends JavaPlugin {

    private YamlDocument mainMenu;
    private YamlDocument messages;
    private YamlDocument quests;
    private YamlDocument settings;

    private DatabaseProvider databaseProvider;
    private QuestManager questManager;

    @Override
    public void onEnable() {
        AliienCore.init(this);

        if (!loadConfig()) {
            getLogger().log(Level.SEVERE, "Failed to load configurations!");
            getServer().getPluginManager().disablePlugin(this);
        }

        setupDatabase();
        this.questManager = new QuestManager(this);
        registerListeners();
    }

    @Override
    public void onDisable() {
        if (AliienCore.getDatabase() != null && databaseProvider != null) AliienCore.getDatabase().disconnect();

        getLogger().info("AliienCommunityQuests has been disabled successfully!");
    }

    /**
     * Loads up all the config files
     *
     * @return {@code true} if the config files are successfully loaded up, {@code false otherwise}
     */
    public boolean loadConfig() {
        try {
            this.mainMenu = ConfigManager.loadConfig(this, "main-menu.yml");
            ConfigManager.bindConfig(this.mainMenu, MainMenu.class);
            MainMenu.load(this.mainMenu);

            this.messages = ConfigManager.loadConfig(this, "messages.yml");
            ConfigManager.bindConfig(this.messages, Messages.class);

            this.quests = ConfigManager.loadConfig(this, "quests.yml");
            Quests.load(this.quests);

            this.settings = ConfigManager.loadConfig(this, "settings.yml");
            ConfigManager.bindConfig(this.settings, Settings.class);

            return true;
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to load or update configuration files!", e);
            return false;
        }
    }

    /**
     * Registers all the quest listeners when the plugin enables
     */
    public void registerListeners() {
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new BreedingListener(this), this);
        getServer().getPluginManager().registerEvents(new ConsumingListener(this), this);
        getServer().getPluginManager().registerEvents(new CraftingListener(this), this);
        getServer().getPluginManager().registerEvents(new EnchantingListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityKillListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerFishListener(this), this);
        getServer().getPluginManager().registerEvents(new SmeltingListener(this), this);
        getServer().getPluginManager().registerEvents(new TamingListener(this), this);
        getServer().getPluginManager().registerEvents(new VillagerTradingListener(this), this);
    }

    /**
     * Sets up the database type based on the user input in the settings.yml file
     */
    private void setupDatabase() {
        databaseProvider = switch (settings.getString("database.type").toUpperCase(Locale.ROOT)) {
        case "H2" -> {
            AliienCore.getDatabase().connectH2(this, "database");
            yield new H2(this);
        }
        case "SQLITE" -> {
            AliienCore.getDatabase().connectSQLite(this, "database");
            yield new SQLite(this);
        }
        case "MYSQL" -> {
            AliienCore.getDatabase().connectMySQL(
                    settings.getString("database.settings.host", "localhost"),
                    settings.getInt("database.settings.port", 3306),
                    settings.getString("database.settings.database", "server"),
                    settings.getString("database.settings.username", "root"),
                    settings.getString("database.settings.password", "password"),
                    settings.getInt("database.settings.advanced.max-pool-size", 10),
                    settings.getInt("database.settings.advanced.min-idle", 10),
                    settings.getLong("database.settings.advanced.connection-timeout", 10000L),
                    settings.getLong("database.settings.advanced.max-lifetime", 1800000L)
            );
            yield new MySQL();
        }
        case "MARIADB" -> {
            AliienCore.getDatabase().connectMariaDB(
                    settings.getString("database.settings.host", "localhost"),
                    settings.getInt("database.settings.port", 3306),
                    settings.getString("database.settings.database", "server"),
                    settings.getString("database.settings.username", "root"),
                    settings.getString("database.settings.password", "password"),
                    settings.getInt("database.settings.advanced.max-pool-size", 10),
                    settings.getInt("database.settings.advanced.min-idle", 10),
                    settings.getLong("database.settings.advanced.connection-timeout", 10000L),
                    settings.getLong("database.settings.advanced.max-lifetime", 1800000L)
            );
            yield new MariaDB();
        }
        default -> {
            getLogger().warning("Invalid database type detected, therefore defaulting to H2. If you are sure that you have typed your storage type correctly and this message is showing up, then this is a bug and must be reported!");
            AliienCore.getDatabase().connectH2(this, "database");
            yield new H2(this);
        }
    };
        this.databaseProvider.init();
    }

    /**
     * @return the database provider
     */
    public DatabaseProvider getDatabaseProvider() {
        return databaseProvider;
    }
}
