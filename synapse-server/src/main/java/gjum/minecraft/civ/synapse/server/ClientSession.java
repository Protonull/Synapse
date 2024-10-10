package gjum.minecraft.civ.synapse.server;

import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import gjum.minecraft.civ.synapse.common.network.packets.UnexpectedPacketException;
import gjum.minecraft.civ.synapse.common.network.packets.clientbound.ClientboundKick;
import gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundBeginHandshake;
import gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundEncryptionResponse;
import gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundIdentityResponse;
import gjum.minecraft.civ.synapse.server.states.ServerConnectionState;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSession.class);
    public static final AttributeKey<ClientSession> KEY = AttributeKey.valueOf(ClientSession.class, "client");
    private static long lastClientId = 0;

    public final long id = ++lastClientId;
    public final Channel channel;

    public ClientSession(
        final @NotNull Channel channel
    ) {
        this.channel = Objects.requireNonNull(channel);
    }

    public @NotNull String getLoggerName() {
        return "Client" + this.id;
    }

    public synchronized void handlePacket(
        final @NotNull Packet received
    ) throws Exception {
        LOGGER.debug("[{}] Received: {}", getLoggerName(), received);
        switch (received) {
            // Handshake
            case final ServerboundBeginHandshake packet -> ServerConnectionState.handleBeginHandshake(this, packet);
            case final ServerboundEncryptionResponse packet -> ServerConnectionState.handleEncryptionResponse(this, packet);
            case final ServerboundIdentityResponse packet -> ServerConnectionState.handleIdentityResponse(this, packet);
            default -> throw new UnexpectedPacketException(received);
        }
    }

    public synchronized void send(
        final @NotNull Packet packet
    ) {
        if (this.channel.isOpen()) {
            LOGGER.debug("[{}] Sending: {}", getLoggerName(), packet);
            this.channel.writeAndFlush(packet);
            return;
        }
        LOGGER.warn("[{}] Connection already closed; dropping packet: {}", getLoggerName(), packet);
    }

    public synchronized void kick(
        final @NotNull String serverReason,
        final @NotNull String clientReason
    ) {
        LOGGER.info("[{}] Being kicked: {}", getLoggerName(), serverReason);
        send(new ClientboundKick(clientReason));
        this.channel.disconnect();
    }

    public synchronized void kick(
        final @NotNull Throwable cause,
        final @NotNull String clientReason
    ) {
        LOGGER.warn("[{}] Being kicked", getLoggerName(), cause);
        send(new ClientboundKick(clientReason));
        this.channel.disconnect();
    }
}
