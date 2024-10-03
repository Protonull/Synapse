package gjum.minecraft.civ.synapse.mod.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import gjum.minecraft.civ.synapse.mod.LiteModSynapse;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
	@Inject(
		method = "handleSystemChat",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V",
			shift = At.Shift.AFTER
		),
		cancellable = true
	)
	protected void onHandleChat(
		final @NotNull ClientboundSystemChatPacket packet,
		final @NotNull CallbackInfo ci,
		final @Local(argsOnly = true) LocalRef<ClientboundSystemChatPacket> packetRef
	) {
		final Component replacement = LiteModSynapse.instance.handleChat(packet.content());
		if (replacement == null) {
			ci.cancel(); // drop packet
			return;
		}
		if (replacement.equals(packet.content())) {
			return; // no change
		}
		packetRef.set(new ClientboundSystemChatPacket(
			replacement,
			packet.overlay()
		));
	}
}
