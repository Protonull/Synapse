package gjum.minecraft.civ.synapse.mod.config;

import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.autogen.AutoGen;

public final class HudConfig {
    public static final String CATEGORY = "synapse.config.category.hud";

    @AutoGen(category = CATEGORY)
    @SerialEntry
    public volatile boolean showHealthPotCount = true;

    @AutoGen(category = CATEGORY)
    @SerialEntry
    public volatile boolean showNearbyHostileCount = true;

    @AutoGen(category = CATEGORY)
    @SerialEntry
    public volatile boolean showNearbyFriendlyCount = true;

    @AutoGen(category = CATEGORY)
    @SerialEntry
    public volatile boolean showNearbyPlayerCount = true;
}
