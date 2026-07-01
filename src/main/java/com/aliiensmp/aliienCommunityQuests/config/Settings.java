package com.aliiensmp.aliienCommunityQuests.config;

import com.aliiensmp.core.config.Key;
import com.aliiensmp.core.utils.DurationUtils;

public class Settings {

    @Key("check-for-updates")
    public static final boolean CHECK_FOR_UPDATES = true;

    @Key("time-display")
    public static final String TIME_STYLE = "SHORT";

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
}
