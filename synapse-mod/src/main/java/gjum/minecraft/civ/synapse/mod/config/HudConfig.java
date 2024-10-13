package gjum.minecraft.civ.synapse.mod.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class HudConfig {
    @SerialEntry
    public volatile boolean showHealthPotCount = true;
    private static @NotNull Option<?> showHealthPotCountOption(
        final @NotNull HudConfig instance,
        final @NotNull HudConfig defaults
    ) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("synapse.config.option.hud.showHealthPotCount"))
            .description(OptionDescription.of(Component.translatable("synapse.config.option.hud.showHealthPotCount.tooltip")))
            .controller(BooleanControllerBuilder::create)
            .binding(
                defaults.showHealthPotCount,
                () -> instance.showHealthPotCount,
                (show) -> instance.showHealthPotCount = show
            )
            .build();
    }

    @SerialEntry
    public volatile boolean showNearbyHostileCount = true;
    private static @NotNull Option<?> showNearbyHostileCountOption(
        final @NotNull HudConfig instance,
        final @NotNull HudConfig defaults
    ) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("synapse.config.option.hud.showNearbyHostileCount"))
            .description(OptionDescription.of(Component.translatable("synapse.config.option.hud.showNearbyHostileCount.tooltip")))
            .controller(BooleanControllerBuilder::create)
            .binding(
                defaults.showNearbyHostileCount,
                () -> instance.showNearbyHostileCount,
                (show) -> instance.showNearbyHostileCount = show
            )
            .build();
    }

    @SerialEntry
    public volatile boolean showNearbyFriendlyCount = true;
    private static @NotNull Option<?> showNearbyFriendlyCountOption(
        final @NotNull HudConfig instance,
        final @NotNull HudConfig defaults
    ) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("synapse.config.option.hud.showNearbyFriendlyCount"))
            .description(OptionDescription.of(Component.translatable("synapse.config.option.hud.showNearbyFriendlyCount.tooltip")))
            .controller(BooleanControllerBuilder::create)
            .binding(
                defaults.showNearbyFriendlyCount,
                () -> instance.showNearbyFriendlyCount,
                (show) -> instance.showNearbyFriendlyCount = show
            )
            .build();
    }

    @SerialEntry
    public volatile boolean showNearbyPlayerCount = true;
    private static @NotNull Option<?> showNearbyPlayerCountOption(
        final @NotNull HudConfig instance,
        final @NotNull HudConfig defaults
    ) {
        return Option.<Boolean>createBuilder()
            .name(Component.translatable("synapse.config.option.hud.showNearbyPlayerCount"))
            .description(OptionDescription.of(Component.translatable("synapse.config.option.hud.showNearbyPlayerCount.tooltip")))
            .controller(BooleanControllerBuilder::create)
            .binding(
                defaults.showNearbyPlayerCount,
                () -> instance.showNearbyPlayerCount,
                (show) -> instance.showNearbyPlayerCount = show
            )
            .build();
    }

    // ============================================================
    // Screen generation
    // ============================================================

    static @NotNull ConfigCategory generateCategory(
        final @NotNull HudConfig hudConfig,
        final @NotNull HudConfig defaults
    ) {
        return ConfigCategory.createBuilder()
            .name(Component.translatable("synapse.config.category.hud"))
            .option(showHealthPotCountOption(hudConfig, defaults))
            .option(showNearbyHostileCountOption(hudConfig, defaults))
            .option(showNearbyFriendlyCountOption(hudConfig, defaults))
            .option(showNearbyPlayerCountOption(hudConfig, defaults))
            .build();
    }
}
