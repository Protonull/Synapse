package gjum.minecraft.civ.synapse.mod.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import gjum.minecraft.civ.synapse.mod.Standing;
import java.awt.Color;
import java.util.EnumMap;
import java.util.stream.Stream;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * {@link gjum.minecraft.civ.synapse.mod.Standing}
 */
public final class StandingConfig extends EnumMap<Standing, Color> {
    public StandingConfig() {
        super(Standing.class);
        put(Standing.HOSTILE, Color.RED);
        put(Standing.FRIENDLY, Color.GREEN);
        put(Standing.NEUTRAL, Color.YELLOW);
        put(Standing.UNSET, Color.WHITE);
    }

    // ============================================================
    // Screen generation
    // ============================================================

    static @NotNull ConfigCategory generateCategory(
        final @NotNull StandingConfig instance,
        final @NotNull StandingConfig defaults
    ) {
        return ConfigCategory.createBuilder()
            .name(Component.translatable("synapse.config.category.standing"))
            .options(
                Stream.of(Standing.values())
                    .map((standing) -> {
                        return Option.<Color>createBuilder()
                            .name(Component.translatable("synapse.config.option.standing.colour", standing.name()))
                            .description(switch (standing) {
                                case HOSTILE -> OptionDescription.of(Component.translatable("synapse.config.option.standing.colour.hostile.tooltip"));
                                case FRIENDLY -> OptionDescription.of(Component.translatable("synapse.config.option.standing.colour.friendly.tooltip"));
                                case NEUTRAL -> OptionDescription.of(Component.translatable("synapse.config.option.standing.colour.neutral.tooltip"));
                                case UNSET -> OptionDescription.of(Component.translatable("synapse.config.option.standing.colour.unset.tooltip"));
                            })
                            .controller((opt) -> ColorControllerBuilder.create(opt).allowAlpha(false))
                            .binding(
                                defaults.getOrDefault(standing, Color.WHITE),
                                () -> instance.getOrDefault(standing, Color.WHITE),
                                (colour) -> instance.put(standing, colour)
                            )
                            .build();
                    })
                    .toList()
            )
            .build();
    }
}
