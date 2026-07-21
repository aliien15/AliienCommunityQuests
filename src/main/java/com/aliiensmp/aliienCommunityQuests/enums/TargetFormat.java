package com.aliiensmp.aliienCommunityQuests.enums;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum TargetFormat {
    LOWERCASE,
    UPPERCASE,
    TITLE_CASE,
    SMALL_CAPS,
    RAW;

    private static final String NORMAL_CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final String SMALL_CAPS_CHARS = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘꞯʀѕᴛᴜᴠᴡxʏᴢ";

    /**
     * Applies the selected formatting rules to the raw target string.
     */
    public String apply(final String rawTarget) {
        return switch (this) {
            case LOWERCASE -> rawTarget.toLowerCase(Locale.ROOT).replace("_", " ");
            case UPPERCASE -> rawTarget.toUpperCase(Locale.ROOT).replace("_", " ");
            case RAW -> rawTarget;
            case SMALL_CAPS -> rawTarget.toLowerCase(Locale.ROOT).replace("_", " ").chars()
                    .mapToObj(c -> {
                        int index = NORMAL_CHARS.indexOf(c);
                        return index != -1 ? String.valueOf(SMALL_CAPS_CHARS.charAt(index)) : String.valueOf((char) c);
                    })
                    .collect(Collectors.joining());
            case TITLE_CASE -> Arrays.stream(rawTarget.split("_"))
                    .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                    .collect(Collectors.joining(" "));
        };
    }

    /**
     * Parses the format from a string, defaulting to TITLE_CASE if invalid.
     */
    public static TargetFormat fromString(final String format) {
        return Arrays.stream(values())
                .filter(v -> v.name().equalsIgnoreCase(format))
                .findFirst()
                .orElse(TITLE_CASE);
    }
}