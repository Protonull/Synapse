package gjum.minecraft.civ.synapse.mod.network;

import com.google.common.net.HostAndPort;
import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import gjum.minecraft.civ.synapse.common.network.packets.PacketHelpers;
import gjum.minecraft.civ.synapse.common.network.packets.UnexpectedPacketException;
import gjum.minecraft.civ.synapse.common.network.packets.clientbound.ClientboundEncryptionRequest;
import gjum.minecraft.civ.synapse.common.network.packets.clientbound.ClientboundIdentityRequest;
import gjum.minecraft.civ.synapse.common.network.protocols.ClientboundProtocol;
import gjum.minecraft.civ.synapse.common.network.protocols.ServerboundProtocol;
import gjum.minecraft.civ.synapse.mod.network.states.ClientConnectionState;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Client {
    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);
    public static final AttributeKey<Client> KEY = AttributeKey.valueOf(Client.class, "client");

    public final Channel channel;
    public final HostAndPort address;

    public Client(
        final @NotNull Channel channel,
        final @NotNull HostAndPort address
    ) {
        this.channel = Objects.requireNonNull(channel);
        this.address = Objects.requireNonNull(address);
    }

    public synchronized void handlePacket(
        final @NotNull Packet received
    ) throws Exception {
        LOGGER.info("Received from [{}]: {}", this.address, received); // TODO: Find a way to make DEBUG work
        switch (received) {
            case final ClientboundEncryptionRequest packet -> ClientConnectionState.handleEncryptionRequest(this, packet);
            case final ClientboundIdentityRequest packet -> ClientConnectionState.handleIdentityRequest(this, packet);
            default -> {} // break
        }
        throw new UnexpectedPacketException(received);
    }

    public synchronized void send(
        final @NotNull Packet packet
    ) {
        if (this.channel.isOpen()) {
            LOGGER.info("Sending to [{}]: {}", this.address, packet); // TODO: Find a way to make DEBUG work
            this.channel.writeAndFlush(packet);
            return;
        }
        LOGGER.warn("Connection to [{}] already closed; dropping packet: {}", this.address, packet);
    }

    public synchronized void disconnect(
        final @NotNull String reason
    ) {
        LOGGER.info("Disconnecting from [{}]: {}", this.address, reason);
        this.channel.disconnect();
    }

    public synchronized void disconnect(
        final @NotNull Throwable cause
    ) {
        LOGGER.info("Disconnecting from [{}]", this.address, cause);
        this.channel.disconnect();
    }

    public synchronized void deinit() {
        this.channel.close();
        this.channel.attr(KEY).set(null);
    }

    public static @NotNull Client connect(
        final @NotNull HostAndPort address
    ) {
        final var bootstrap = new Bootstrap();
        bootstrap.group(net.minecraft.network.Connection.NETWORK_WORKER_GROUP.get())
            .channel(NioSocketChannel.class)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(
                    final @NotNull SocketChannel ch
                ) {
                    final Client client = requireClient(ch);

                    ch.pipeline()
                        .addLast(PacketHelpers.generatePacketLengthPrefixHandlers())
                        .addLast("decoder", new ClientboundProtocol.Decoder())
                        .addLast("encoder", new ServerboundProtocol.Encoder())
                        .addLast("handler", new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelActive(
                                final @NotNull ChannelHandlerContext ctx
                            ) throws Exception {
                                LOGGER.info("Connected to [{}]", address);
                                ClientConnectionState.handleConnected(client);
                            }
                            @Override
                            public void channelInactive(
                                final @NotNull ChannelHandlerContext ctx
                            ) throws Exception {
                                LOGGER.info("Disconnected from [{}]", address);
                            }
                            @Override
                            public void channelRead(
                                final @NotNull ChannelHandlerContext ctx,
                                final @NotNull Object received
                            ) throws Exception {
                                if (received instanceof final Packet packet) {
                                    LOGGER.info("Receiving packet: {}", packet.getClass().getSimpleName());
                                    client.handlePacket(packet);
                                    LOGGER.info("Handled packet: {}", packet.getClass().getSimpleName());
                                    return;
                                }
                                LOGGER.info("Received non-packet!: {}", received.getClass().getName());
                            }
                            @Override
                            public void exceptionCaught(
                                final @NotNull ChannelHandlerContext ctx,
                                final @NotNull Throwable cause
                            ) {
                                if (cause instanceof IOException && "Connection reset by peer".equals(cause.getMessage())) {
                                    LOGGER.warn("CONNECTION RESET", cause);
                                    return;
                                }
                                if (cause instanceof ConnectException && cause.getMessage().startsWith("Connection refused: ")) {
                                    LOGGER.warn("CONNECTION REFUSED", cause);
                                    return;
                                }
                                LOGGER.warn("", cause);
                            }
                        });
                }
            });
        final ChannelFuture channelFuture = bootstrap.connect(address.getHost(), address.getPort());
        final var client = new Client(channelFuture.channel(), address);
        client.channel.attr(KEY).set(client);
        return client;
    }

    public static @NotNull Client requireClient(
        final @NotNull Channel channel
    ) {
        return Objects.requireNonNull(channel.attr(KEY).get());
    }

    public interface Holder {
        @Nullable Client synapse$getConnection();

        void synapse$setConnection(
            Client client
        );
    }
}
