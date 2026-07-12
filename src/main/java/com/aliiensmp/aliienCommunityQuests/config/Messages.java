package com.aliiensmp.aliienCommunityQuests.config;

import com.aliiensmp.core.config.Key;

public class Messages {

    @Key("prefix")
    public static final String PREFIX = "&6&lAliienCommunityQuests &r&8| &6";

    @Key("reload.reloading")
    public static final String RELOAD_RELOADING = "Reloading AliienCommunityQuests...";

    @Key("reload.fail")
    public static final String RELOAD_FAIL = "Failed to reload AliienCommunityQuests (check console for errors)";

    @Key("reload.success")
    public static final String RELOAD_SUCCESS = "Successfully reloaded AliienCommunityQuests!";

    @Key("rewards.claimed")
    public static final String REWARDS_CLAIMED = "You have successfully claimed your rewards!";

    @Key("rewards.not-found")
    public static final String REWARDS_NOT_FOUND = "You do not have any rewards to claim!";

    @Key("quests.started")
    public static final String QUEST_STARTED = "A new community quest has just started!";

    @Key("quests.completed")
    public static final String QUEST_COMPLETED = "A community quest has just been completed!";

    @Key("quests.ended")
    public static final String QUEST_ENDED = "A community quest has just ended, and unfortunately it was not completed :(";

    @Key("quests.reset")
    public static final String QUEST_RESET = "This quest has been reset by an admin!";

    @Key("quests.not-found")
    public static final String QUEST_NOT_FOUND = "This quest does not exist!";

    @Key("no-perms")
    public static final String NO_PERMS = "You do not have permission to do this!";

    @Key("new-update")
    public static final String NEW_UPDATE = "A new AliienCommunityQuests update is now available!";
}
