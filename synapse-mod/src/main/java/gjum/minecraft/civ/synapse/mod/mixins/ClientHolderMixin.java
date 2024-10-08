package gjum.minecraft.civ.synapse.mod.mixins;

import com.google.common.net.HostAndPort;
import gjum.minecraft.civ.synapse.mod.network.Client;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientHolderMixin implements Client.Holder {
    @Unique
    private Client client = null;

    @Override
    public @Nullable Client synapse$getConnection() {
        return this.client;
    }

    @Override
    public void synapse$setConnection(
        final Client client
    ) {
        this.client = client;
    }

    @Inject(
        method = "handleLogin",
        at = @At("TAIL")
    )
    protected void synapse$autoConnectOnJoin(
        final @NotNull ClientboundLoginPacket packet,
        final @NotNull CallbackInfo ci
    ) {
        // TODO: Add an 'auto-connect' option
        synapse$setConnection(Client.connect(HostAndPort.fromString("localhost:22001").withDefaultPort(22001)));
    }

    @Inject(
        method = "close",
        at = @At("TAIL")
    )
    protected void synapse$onClose(
        final @NotNull CallbackInfo ci
    ) {
        final Client client = synapse$getConnection();
        if (client != null) {
            if (client.channel.isOpen()) {
                client.channel.close();
            }
            synapse$setConnection(null);
        }
    }
}
