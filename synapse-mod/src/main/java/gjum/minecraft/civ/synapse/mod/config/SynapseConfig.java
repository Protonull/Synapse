package gjum.minecraft.civ.synapse.mod.config;

import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public final class SynapseConfig {
    @SerialEntry(value = "hud")
    public final HudConfig hudConfig = new HudConfig();

    // ============================================================
    // Serialisation
    // ============================================================

    public static final ConfigClassHandler<SynapseConfig> HANDLER = ConfigClassHandler.createBuilder(SynapseConfig.class)
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

    public static @NotNull YetAnotherConfigLib newScreenGenerator() {
        final SynapseConfig instance = HANDLER.instance(), defaults = HANDLER.defaults();
        return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("category.synapse"))
            .category(HudConfig.generateCategory(instance.hudConfig, defaults.hudConfig))
            .save(HANDLER::save)
            .build();
    }
}