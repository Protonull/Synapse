package gjum.minecraft.civ.synapse.mod.config;

import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.autogen.AutoGen;
import dev.isxander.yacl3.config.v2.api.autogen.TickBox;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public final class SynapseConfig {
    // ============================================================
    // Hud Config
    // ============================================================

    public static final String HUD_CATEGORY = "synapse.config.category.hud";

    @SerialEntry(value = "hud.showHealthPotCount")
    @AutoGen(category = HUD_CATEGORY)
    @TickBox
    public volatile boolean hudShowHealthPotCount = true;

    @SerialEntry(value = "hud.showNearbyHostileCount")
    @AutoGen(category = HUD_CATEGORY)
    @TickBox
    public volatile boolean hudShowNearbyHostileCount = true;

    @SerialEntry(value = "hud.showNearbyFriendlyCount")
    @AutoGen(category = HUD_CATEGORY)
    @TickBox
    public volatile boolean hudShowNearbyFriendlyCount = true;

    @SerialEntry(value = "hud.showNearbyPlayerCount")
    @AutoGen(category = HUD_CATEGORY)
    @TickBox
    public volatile boolean hudShowNearbyPlayerCount = true;

    // ============================================================
    // Serialisation
    // ============================================================

    public static ConfigClassHandler<SynapseConfig> HANDLER = ConfigClassHandler.createBuilder(SynapseConfig.class)
        .id(ResourceLocation.tryBuild("synapse", "config"))
        .serializer((config) -> {
            return GsonConfigSerializerBuilder.create(config)
                .setPath(FabricLoader.getInstance().getConfigDir().resolve("synapse.json"))
                .setJson5(false)
                .build();
        })
        .build();

    // ============================================================
    // Screen generation
    // ============================================================

    public static @NotNull YetAnotherConfigLib newScreenGenerator(
        final @NotNull SynapseConfig config
    ) {
        return HANDLER.generateGui();
    }
}