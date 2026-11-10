import com.aliiensmp.aliienCommunityQuests.core.AliienCore;
import com.aliiensmp.aliienCommunityQuests.core.lib.hikari.HikariConfig;
import com.aliiensmp.aliienCommunityQuests.database.DatabaseProvider;
import com.aliiensmp.aliienCommunityQuests.database.options.H2;
import com.aliiensmp.aliienCommunityQuests.database.options.SQLite;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Run against the final shaded JAR using verify-integration.py. */
public class IntegrationSmoke {
    public static void main(String[] args) throws Exception {
        var expectedDrivers =
                Set.of(
                        "com.mysql.cj.jdbc.Driver",
                        "org.mariadb.jdbc.Driver",
                        "org.h2.Driver",
                        "org.sqlite.JDBC");
        var loadedDrivers = new java.util.HashSet<String>();
        for (var driver : java.util.ServiceLoader.load(java.sql.Driver.class)) {
            if (expectedDrivers.contains(driver.getClass().getName())) {
                check(
                        !driver.getClass()
                                .getProtectionDomain()
                                .getCodeSource()
                                .getLocation()
                                .equals(
                                        AliienCore.class
                                                .getProtectionDomain()
                                                .getCodeSource()
                                                .getLocation()),
                        "Driver must be loaded externally, not from the plugin JAR");
                loadedDrivers.add(driver.getClass().getName());
            }
        }
        check(loadedDrivers.equals(expectedDrivers), "All four external drivers must load");
        System.out.println("All four external JDBC providers loaded");
        for (boolean sqlite : new boolean[] {false, true}) {
            for (boolean legacy : new boolean[] {false, true}) {
                var config = new HikariConfig();
                config.setJdbcUrl(
                        sqlite ? "jdbc:sqlite::memory:" : "jdbc:h2:mem:quests;MODE=MySQL");
                config.setMaximumPoolSize(1);
                var db = AliienCore.getDatabase();
                db.connect(config);
                try {
                    DatabaseProvider provider =
                            sqlite ? new SQLite(null, "cq_") : new H2(null, "cq_");
                    if (legacy)
                        check(
                                db.executeAsync(
                                                "CREATE TABLE cq_active_quest_participants"
                                                        + " (quest_id VARCHAR(255), player_uuid"
                                                        + " VARCHAR(36), PRIMARY KEY (quest_id,"
                                                        + " player_uuid))")
                                        .join(),
                                "legacy schema");
                    provider.init().join();
                    provider.init().join(); // Restart must not fail on duplicate columns.
                    UUID player = UUID.randomUUID();
                    provider.saveActiveQuest(
                                    "quest", Map.of("objective", 7), Map.of(player, 7), 123456L)
                            .join();
                    var state = provider.loadActiveCache().join().get("quest");
                    check(state.objectiveProgress().get("objective") == 7, "progress round trip");
                    check(
                            state.participantsWithContributions().get(player) == 7,
                            "contribution round trip");
                    check(state.endTime() == 123456L, "expiry round trip");
                    provider.grantRewards(Set.of(player), "give %player% diamond").join();
                    check(
                            provider.getPendingRewards(player)
                                    .join()
                                    .equals(java.util.List.of("give %player% diamond")),
                            "reward round trip");
                    check(provider.clearPendingRewards(player).join(), "clear rewards");
                    check(provider.getPendingRewards(player).join().isEmpty(), "rewards removed");
                    check(provider.clearActiveQuestBackup("quest").join(), "clear quest");
                    check(provider.loadActiveCache().join().isEmpty(), "quest removed");
                    System.out.println(
                            (sqlite ? "SQLite" : "H2")
                                    + (legacy ? " legacy" : " fresh")
                                    + " passed");
                } finally {
                    db.disconnect();
                }
            }
        }
        try {
            new H2(null, "cq_").grantRewards(Set.of(UUID.randomUUID()), "reward").join();
            throw new AssertionError("Write failure was swallowed");
        } catch (java.util.concurrent.CompletionException expected) {
            System.out.println("Write failure propagation passed");
        }
        // A disconnected manager returns false; schema init must surface the failure.
        try {
            new H2(null, "cq_").init().join();
            throw new AssertionError("Schema failure was swallowed");
        } catch (java.util.concurrent.CompletionException expected) {
            System.out.println("Schema failure propagation passed");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
