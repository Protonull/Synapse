package gjum.minecraft.civ.synapse.server;

import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import io.netty.channel.Channel;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClientSession {
    public final long connectTime = System.currentTimeMillis();
    public @NotNull final Channel channel;
    public @Nullable String synapseVersion = null;
    /**
     * arbitrary, as sent by client during handshake. use with caution
     */
    public @Nullable String claimedUsername = null;
    public byte @Nullable [] verifyToken = null;
    private @Nullable UUID mojangUuid = null;
    private @Nullable String mojangUsername = null;
    private @Nullable String civUsername = null;
    public boolean whitelisted = false;
    public @Nullable String gameAddress = null;
    public @Nullable String disconnectReason = null;

    public ClientSession(
        final @NotNull Channel channel
    ) {
        this.channel = Objects.requireNonNull(channel);
    }

    public @Nullable UUID getMojangUuid() {
        return this.mojangUuid;
    }

    public @Nullable String getMojangUsername() {
        return this.mojangUsername;
    }

    public void setAccountInfo(
        final @Nullable UUID mojangUuid,
        final @Nullable String mojangAccount,
        final @Nullable String civRealmsAccount
    ) {
        this.mojangUuid = mojangUuid;
        this.mojangUsername = mojangAccount;
        this.civUsername = civRealmsAccount;
    }

    public @Nullable String getCivUsername() {
        return this.civUsername;
    }

    public boolean isHandshaked() {
        return this.verifyToken != null;
    }

    public boolean isAuthenticated() {
        return this.mojangUuid != null && this.mojangUsername != null;
    }

    public void send(
        final @NotNull Packet packet
    ) {
        if (this.channel.isOpen()) {
            this.channel.writeAndFlush(packet);
        }
        else {
            Server.log(this, Level.WARNING, "Connection already closed; dropping packet " + packet);
        }
    }

    public void addDisconnectReason(
        String reason
    ) {
        if (this.disconnectReason != null) {
            reason += " - " + this.disconnectReason;
        }
        this.disconnectReason = reason;
    }
}
