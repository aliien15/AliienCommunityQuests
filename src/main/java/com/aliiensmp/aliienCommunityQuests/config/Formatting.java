package com.aliiensmp.aliienCommunityQuests.config;

import com.aliiensmp.aliienCommunityQuests.config.records.Objective;
import com.aliiensmp.aliienCommunityQuests.enums.ObjectiveType;
import com.aliiensmp.aliienCommunityQuests.enums.TargetFormat;
import com.aliiensmp.core.lib.boostedyaml.YamlDocument;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Formatting {

    private static final Map<ObjectiveType, String> ACTIONS = new EnumMap<>(ObjectiveType.class);
    private static final Map<String, String> TARGETS = new ConcurrentHashMap<>();

    private static TargetFormat defaultFormat = TargetFormat.TITLE_CASE;

    public static void load(final YamlDocument config) {
        ACTIONS.clear();
        TARGETS.clear();

        defaultFormat = TargetFormat.fromString(config.getString("unlisted-target-format", "TITLE_CASE"));

        Optional.ofNullable(config.getSection("actions")).ifPresent(section ->
                section.getRoutesAsStrings(false).forEach(key -> {
                    try {
                        ACTIONS.put(ObjectiveType.valueOf(key.toUpperCase()), section.getString(key));
                    } catch (IllegalArgumentException ignored) {}
                })
        );

        Optional.ofNullable(config.getSection("targets")).ifPresent(section ->
                section.getRoutesAsStrings(false).forEach(key ->
                        TARGETS.put(key, section.getString(key))
                )
        );
    }

    public static String formatObjective(final Objective objective) {
        final String verb = ACTIONS.getOrDefault(objective.type(), "Complete");

        final String targetFormat = TARGETS.computeIfAbsent(objective.target(), defaultFormat::apply);

        return verb + " " + targetFormat;
    }
}