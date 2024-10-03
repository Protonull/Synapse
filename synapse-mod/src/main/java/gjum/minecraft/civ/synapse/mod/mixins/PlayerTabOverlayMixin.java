package gjum.minecraft.civ.synapse.mod.mixins;

import gjum.minecraft.civ.synapse.mod.LiteModSynapse;
import gjum.minecraft.civ.synapse.mod.McUtil;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
    @Inject(
        method = "getNameForDisplay",
        at = @At("HEAD"),
        cancellable = true
    )
    protected void getPlayerNameHandler(
        final @NotNull PlayerInfo playerInfo,
        final @NotNull CallbackInfoReturnable<Component> cir
    ) {
        if (!LiteModSynapse.instance.isModActive()) {
            return;
        }
        if (!LiteModSynapse.instance.config.isReplaceTablistColors()) {
            return;
        }
        final Component displayName = LiteModSynapse.instance.getDisplayNameForAccount(
            McUtil.getDisplayNameFromTablist(playerInfo)
        );
        if (displayName != null) {
            cir.setReturnValue(displayName);
        }
    }
}
