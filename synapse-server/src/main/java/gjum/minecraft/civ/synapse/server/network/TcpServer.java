package gjum.minecraft.civ.synapse.server.network;

import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import gjum.minecraft.civ.synapse.common.network.packets.PacketHelpers;
import gjum.minecraft.civ.synapse.common.network.packets.UnexpectedPacketException;
import gjum.minecraft.civ.synapse.common.network.protocols.ClientboundProtocol;
import gjum.minecraft.civ.synapse.common.network.protocols.ServerboundProtocol;
import gjum.minecraft.civ.synapse.server.config.ServerEnvironment;
import gjum.minecraft.civ.synapse.server.network.states.ServerConnectionState;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TcpServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TcpServer.class);

    public final Channel channel;
    public final Set<TcpClient> connections;

    private TcpServer(
        final @NotNull Channel channel,
        final @NotNull Set<@NotNull TcpClient> connections
    ) {
        this.channel = Objects.requireNonNull(channel);
        this.connections = Collections.unmodifiableSet(connections);
    }

    public static @NotNull TcpServer start() {
        final var connections = Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<TcpClient, Boolean>()));
        final var rateLimitTracker = new HashMap<String, LongCollection>();

        final EventLoopGroup parentGroup = new NioEventLoopGroup(), childGroup = new NioEventLoopGroup();
        final var bootstrap = new ServerBootstrap();
        bootstrap.group(parentGroup, childGroup)
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(
                    final @NotNull SocketChannel ch
                ) {
                    final TcpClient client = new TcpClient(ch);
                    ch.attr(TcpClient.KEY).set(client);

                    if (hasHitRateLimit(rateLimitTracker, ch.remoteAddress().getHostString())) {
                        LOGGER.warn(
                            "[{}@{}] Has hit rate limit, disconnecting!",
                            client.getLoggerName(),
                            ch.remoteAddress()
                        );
                        ch.close();
                        return;
                    }

                    ch.pipeline()
                        .addLast(PacketHelpers.generatePacketLengthPrefixHandlers())
                        .addLast("decoder", new ServerboundProtocol.Decoder())
                        .addLast("encoder", new ClientboundProtocol.Encoder())
                        .addLast("handler", new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelActive(
                                final @NotNull ChannelHandlerContext ctx
                            ) throws Exception {
                                LOGGER.info("[{}] Connected", client.getLoggerName());
                                connections.add(client);
                                ServerConnectionState.handleConnected(client);
                            }

                            @Override
                            public void channelInactive(
                                final @NotNull ChannelHandlerContext ctx
                            ) throws Exception {
                                LOGGER.info("[{}] Disconnected", client.getLoggerName());
                                connections.remove(client);
                            }

                            @Override
                            public void exceptionCaught(
                                final @NotNull ChannelHandlerContext ctx,
                                final @NotNull Throwable cause
                            ) {
                                if (cause instanceof IOException && "Connection reset by peer".equals(cause.getMessage())) {
                                    return;
                                }
                                client.kick(cause, "Something went wrong!");
                            }

                            @Override
                            public void channelRead(
                                final @NotNull ChannelHandlerContext ctx,
                                final @NotNull Object received
                            ) throws Exception {
                                if (received instanceof final Packet packet) {
                                    client.handlePacket(packet);
                                    return;
                                }
                                throw new UnexpectedPacketException("Received a non-packet! [" + received + "]");
                            }
                        });
                }
            });

        final ChannelFuture serverFuture = bootstrap.bind(ServerEnvironment.PORT);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            childGroup.shutdownGracefully();
            parentGroup.shutdownGracefully();
        }));

        return new TcpServer(
            serverFuture.channel(),
            connections
        );
    }

    /**
     * @return true if the connection should be denied
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private static boolean hasHitRateLimit(
        final @NotNull Map<@NotNull String, @NotNull LongCollection> tracker,
        final @NotNull String source
    ) {
        synchronized (tracker) {
            final LongCollection lastConnectTimes = tracker.computeIfAbsent(source, (_source) -> new LongArrayList(2));
            final long now = System.currentTimeMillis();
            lastConnectTimes.removeIf((timestamp) -> timestamp < now - ServerEnvironment.CONNECT_RATE_LIMIT_WINDOW);
            lastConnectTimes.add(now);
            return lastConnectTimes.size() > ServerEnvironment.CONNECT_RATE_LIMIT_COUNT;
        }
    }
}
