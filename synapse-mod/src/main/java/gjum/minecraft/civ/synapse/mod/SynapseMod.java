package gjum.minecraft.civ.synapse.mod;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.jetbrains.annotations.ApiStatus;

public final class SynapseMod {
    public static final KeyMapping CHAT_POS_KEYBIND = new KeyMapping(
        "key.synapse.print-position",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        "category.synapse"
    );
    public static final KeyMapping OPEN_GUI_KEYBIND = new KeyMapping(
        "key.synapse.settings",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        "category.synapse"
    );
    public static final KeyMapping TOGGLE_ENABLED_KEYBIND = new KeyMapping(
        "key.synapse.enabled",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        "category.synapse"
    );
    public static final KeyMapping SET_FOCUS_ENTITY_KEYBIND = new KeyMapping(
        "key.synapse.crosshair-focus",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        "category.synapse"
    );

    @ApiStatus.Internal
    public static void bootstrap() {
        KeyBindingHelper.registerKeyBinding(CHAT_POS_KEYBIND);
        KeyBindingHelper.registerKeyBinding(OPEN_GUI_KEYBIND);
        KeyBindingHelper.registerKeyBinding(TOGGLE_ENABLED_KEYBIND);
        KeyBindingHelper.registerKeyBinding(SET_FOCUS_ENTITY_KEYBIND);
    }
}
