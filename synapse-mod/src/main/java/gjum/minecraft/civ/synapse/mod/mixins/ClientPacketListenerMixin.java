package gjum.minecraft.civ.synapse.mod.mixins;

import gjum.minecraft.civ.synapse.mod.LiteModSynapse;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Redirect(
        method = "handleSystemChat",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/chat/ChatListener;handleSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"
        )
    )
    protected void onHandleChat(
        final @NotNull ChatListener instance,
        final @NotNull Component message,
        final boolean isOverlay
    ) {
        final Component replacement = LiteModSynapse.instance.handleChat(message);
        if (replacement == null) {
            return; // drop packet
        }
        instance.handleSystemMessage(replacement, isOverlay);
    }
}
