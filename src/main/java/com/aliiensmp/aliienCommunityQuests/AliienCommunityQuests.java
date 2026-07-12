package com.aliiensmp.aliienCommunityQuests;

import co.aikar.commands.MessageKeys;
import co.aikar.commands.PaperCommandManager;
import com.aliiensmp.aliienCommunityQuests.commands.AdminCommands;
import com.aliiensmp.aliienCommunityQuests.commands.PlayerCommands;
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
import com.aliiensmp.core.utils.ColorUtils;
import com.aliiensmp.core.utils.updatechecker.UpdateChecker;
import com.aliiensmp.core.utils.updatechecker.UpdateNotifyListener;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.PluginManager;
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

    private static final String UPDATE_GIST_URL = "https://gist.githubusercontent.com/aliien15/e36ae7db63b09d5127205fb9a38063e4/raw/AliienCommunityQuests.txt";

    @Override
    public void onEnable() {
        AliienCore.init(this);

        if (!loadConfig()) {
            getLogger().log(Level.SEVERE, "Failed to load configurations, therefore the plugin is shutting down!");
            getServer().getPluginManager().disablePlugin(this);
        }

        setupDatabase();
        this.questManager = new QuestManager(this);
        registerListeners();
        setupCommands();

        setupUpdateChecker();
        setupBstats();

        getLogger().info("AliienCommunityQuests has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (AliienCore.getDatabase() != null && databaseProvider != null) AliienCore.getDatabase().disconnect();

        getLogger().info("AliienCommunityQuests has been disabled successfully!");
    }

    /**
     * Setup bStats hook
     */
    private void setupBstats() {
        Metrics metric = new Metrics(this, 32585);
    }

    /**
     * Starts the version check and join notification flow when enabled.
     */
    private void setupUpdateChecker() {
        if (!Settings.CHECK_FOR_UPDATES) return;

        new UpdateChecker(this, UPDATE_GIST_URL).getVersion(version -> {
            if (this.getPluginMeta().getVersion().equals(version)) {
                getLogger().info("AliienCommunityQuests is up to date!");
            } else {
                getLogger().warning("A new update is available for AliienCommmunityQuests!");
            }
        });

        getServer().getPluginManager().registerEvents(
                new UpdateNotifyListener(
                        this,
                        UPDATE_GIST_URL,
                        "aliien.communityquests.admin",
                        () -> ColorUtils.color(Messages.NEW_UPDATE)
                ),
                this
        );
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
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new BlockBreakListener(this), this);
        pm.registerEvents(new BreedingListener(this), this);
        pm.registerEvents(new ConsumingListener(this), this);
        pm.registerEvents(new CraftingListener(this), this);
        pm.registerEvents(new EnchantingListener(this), this);
        pm.registerEvents(new EntityKillListener(this), this);
        pm.registerEvents(new PlayerFishListener(this), this);
        pm.registerEvents(new SmeltingListener(this), this);
        pm.registerEvents(new TamingListener(this), this);
        pm.registerEvents(new VillagerTradingListener(this), this);
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
     * Sets up the plugin commands
     */
    private void setupCommands() {
        PaperCommandManager commandManager = new PaperCommandManager(this);

        // Prefix
        commandManager.getLocales().addMessage(
                Locale.ENGLISH,
                MessageKeys.ERROR_PREFIX,
                Messages.PREFIX
        );

        // No perms msg
        commandManager.getLocales().addMessage(
                Locale.ENGLISH,
                MessageKeys.PERMISSION_DENIED,
                Messages.NO_PERMS
        );

        // Auto tab
        commandManager.getCommandCompletions().registerCompletion("questId", c -> QuestManager.ACTIVE_QUESTS.keySet());

        // Events registerings
        commandManager.registerCommand(new PlayerCommands(this));
        commandManager.registerCommand(new AdminCommands(this));
    }

    /**
     * @return the database provider
     */
    public DatabaseProvider getDatabaseProvider() {
        return databaseProvider;
    }
    public QuestManager getQuestManager() { return questManager; }
}
