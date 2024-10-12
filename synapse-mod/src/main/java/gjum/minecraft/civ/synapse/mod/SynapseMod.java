package gjum.minecraft.civ.synapse.mod;

import com.mojang.blaze3d.platform.InputConstants;
import gjum.minecraft.civ.synapse.mod.config.SynapseConfig;
import gjum.minecraft.civ.synapse.mod.events.EventBus;
import gjum.minecraft.civ.synapse.mod.features.SynapseHud;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public final class SynapseMod {
    public static final KeyMapping OPEN_GUI_KEYBIND = new KeyMapping(
        "key.synapse.settings",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        "category.synapse"
    );
    public static final KeyMapping CHAT_POS_KEYBIND = new KeyMapping(
        "key.synapse.print-position",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        "category.synapse"
    );

    public static final EventBus EVENTS = new EventBus("Synapse-Main-Thread-Events", Minecraft.getInstance());

    @Deprecated
    public static final LiteModSynapse legacy = new LiteModSynapse();

    @ApiStatus.Internal
    public static void bootstrap() {
        if (!SynapseConfig.HANDLER.load()) {
            throw new IllegalStateException("Could not load Synapse config!");
        }
        
        KeyBindingHelper.registerKeyBinding(CHAT_POS_KEYBIND);
        KeyBindingHelper.registerKeyBinding(OPEN_GUI_KEYBIND);

        HudRenderCallback.EVENT.register(SynapseHud::renderHud);

        ClientTickEvents.START_CLIENT_TICK.register((minecraft) -> {
            while (OPEN_GUI_KEYBIND.consumeClick()) {
                minecraft.setScreen(newConfigScreen(minecraft.screen));
            }
        });
    }

    public static @NotNull Screen newConfigScreen(
        final Screen previousScreen
    ) {
        return SynapseConfig.newScreenGenerator(SynapseConfig.HANDLER.instance()).generateScreen(previousScreen);
    }
}
