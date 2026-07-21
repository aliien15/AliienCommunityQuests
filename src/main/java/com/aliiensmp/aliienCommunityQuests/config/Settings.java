package com.aliiensmp.aliienCommunityQuests.config;

import com.aliiensmp.core.config.Key;
import com.aliiensmp.core.utils.DurationUtils;
import org.jetbrains.annotations.NotNull;

public class Settings {

    @Key("check-for-updates")
    public static boolean CHECK_FOR_UPDATES = true;

    @Key("backup-interval")
    public static @NotNull String BACKUP_INTERVAL = "5m";

    @Key("time-display")
    public static @NotNull String TIME_STYLE = "SHORT";

    /**
     * @return the {@code TIME_STYLE} String converted to a DurationUtils Style
     */
    public static DurationUtils.Style toStyle() {
        return switch (TIME_STYLE) {
            case "LONG" -> DurationUtils.Style.LONG;
            case "CLOCK" -> DurationUtils.Style.CLOCK;
            default -> DurationUtils.Style.SHORT;
        };
    }

    @Key("status.completed")
    public static @NotNull String STATUS_COMPLETED = "&a&lCOMPLETED";

    @Key("status.in-progress")
    public static @NotNull String STATUS_IN_PROGRESS = "&7%current%/%amount%";

    @Key("count-deepslate-ores")
    public static boolean COUNT_DEEPSLATE_ORES = true;
}
