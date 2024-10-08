package gjum.minecraft.civ.synapse.server;

import gjum.minecraft.civ.synapse.common.Util;
import gjum.minecraft.civ.synapse.common.configs.StringParsing;
import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import gjum.minecraft.civ.synapse.common.network.packets.PacketHelpers;
import gjum.minecraft.civ.synapse.common.network.packets.UnexpectedPacketException;
import gjum.minecraft.civ.synapse.common.network.protocols.ClientboundProtocol;
import gjum.minecraft.civ.synapse.common.network.protocols.ServerboundProtocol;
import gjum.minecraft.civ.synapse.common.observations.PlayerTracker;
import gjum.minecraft.civ.synapse.server.config.AccountsListConfig;
import gjum.minecraft.civ.synapse.server.config.UuidsConfig;
import gjum.minecraft.civ.synapse.server.states.ServerConnectionState;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Server {
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    public static final int PORT = StringParsing.parseInt(
        System.getenv("SYNAPSE_PORT"),
        22001
    );
    public static final String GAME_ADDRESS = Util.ensureFullAddress(StringParsing.parse(
        System.getenv("SYNAPSE_GAME_ADDRESS"),
        "play.civmc.net"
    ), 25565);
    public static final boolean REQUIRES_AUTH = StringParsing.parseBoolean(
        System.getenv("SYNAPSE_REQUIRES_AUTH"),
        true
    );
    private static final long STATS_INTERVAL = TimeUnit.SECONDS.toMillis(StringParsing.parseLong(
        System.getenv("SYNAPSE_STATS_INTERVAL"),
        300 // seconds
    ));
    private static final long CONNECT_RATE_LIMIT_WINDOW = TimeUnit.MINUTES.toMillis(StringParsing.parseLong(
        System.getenv("SYNAPSE_CONNECT_RATE_LIMIT_WINDOW"),
        1 // minute
    ));
    private static final int CONNECT_RATE_LIMIT_COUNT = StringParsing.parseInt(
        System.getenv("SYNAPSE_CONNECT_RATE_LIMIT_COUNT"),
        7 // 7 connections over the past rateLimitWindow
    );

    public final UuidsConfig uuidMapper = new UuidsConfig();
    private final AccountsListConfig userList = new AccountsListConfig(this.uuidMapper);
    private final AccountsListConfig adminList = new AccountsListConfig(this.uuidMapper);

    public final Long2ObjectMap<ClientSession> connections = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());
    private final PlayerTracker playerTracker = new PlayerTracker(null);

    private final HashMap<String, LongCollection> rateLimitTracker = new HashMap<>();
    private long ingressCountInLastInterval = 0;
    private long egressCountInLastInterval = 0;

    public static void main(
        final @NotNull String @NotNull [] args
    ) {
        try {
            final var server = new Server();
            server.run();
        }
        catch (final Throwable e) {
            LOGGER.error("", e);
        }
    }

    public Server() {
        // must be loaded first; others depend on it during loading
        this.uuidMapper.load(new File(StringParsing.parse(
            System.getenv("SYNAPSE_UUIDS_PATH"),
            "uuids.tsv"
        )));
        this.uuidMapper.saveLater(null);

        this.userList.load(new File(StringParsing.parse(
            System.getenv("SYNAPSE_USERS_PATH"),
            "users.tsv"
        )));
        this.userList.saveLater(null);

        this.adminList.load(new File(StringParsing.parse(
            System.getenv("SYNAPSE_ADMINS_PATH"),
            "admins.tsv"
        )));
        this.adminList.saveLater(null);

        LOGGER.info(
            "Starting server. PORT={}, GAME_ADDRESS={}, REQUIRES_AUTH={}",
            PORT,
            GAME_ADDRESS,
            REQUIRES_AUTH
        );
    }

    public void run() throws InterruptedException {
        final var parentGroup = new NioEventLoopGroup();
        final var childGroup = new NioEventLoopGroup();
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
                    final ClientSession client = new ClientSession(ch);
                    ch.attr(ClientSession.KEY).set(client);

                    if (checkRateLimit(ch.remoteAddress().getHostString())) {
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
                                Server.this.connections.put(client.id, client);
                                ServerConnectionState.handleConnected(client);
                            }
                            @Override
                            public void channelInactive(
                                final @NotNull ChannelHandlerContext ctx
                            ) throws Exception {
                                LOGGER.info("[{}] Disconnected", client.getLoggerName());
                                Server.this.connections.remove(client.id);
                            }
                            @Override
                            public void exceptionCaught(
                                final @NotNull ChannelHandlerContext ctx,
                                final @NotNull Throwable cause
                            ) {
                                if (cause instanceof IOException && "Connection reset by peer".equals(cause.getMessage())) {
                                    return;
                                }
                                client.kick(cause);
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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            childGroup.shutdownGracefully();
            parentGroup.shutdownGracefully();
        }));

        // Bind and start to accept incoming connections.
        final ChannelFuture serverFuture = bootstrap.bind(PORT).sync();

        final String intervalMinStr = new DecimalFormat("#.#").format(TimeUnit.MILLISECONDS.toMinutes(STATS_INTERVAL)) + "min";
        final long initialDelay = STATS_INTERVAL - (System.currentTimeMillis() % STATS_INTERVAL);
        parentGroup.scheduleAtFixedRate(
            () -> {
                if (this.ingressCountInLastInterval != 0 && this.egressCountInLastInterval != 0) {
                    LOGGER.info(
                        "ingress: {} egress: {} over past {}",
                        this.ingressCountInLastInterval,
                        this.egressCountInLastInterval,
                        intervalMinStr
                    );
                }
                this.ingressCountInLastInterval = 0;
                this.egressCountInLastInterval = 0;
            },
            initialDelay,
            STATS_INTERVAL,
            TimeUnit.MILLISECONDS
        );

        // Wait until the server socket is closed.
        serverFuture.channel().closeFuture().sync();
    }

    /**
     * @return true iff the connection should be denied
     */
    private synchronized boolean checkRateLimit(String source) {
        final LongCollection lastConnectTimes = this.rateLimitTracker.computeIfAbsent(source, (_source) -> new LongArrayList(2));
        final long now = System.currentTimeMillis();
        lastConnectTimes.removeIf((timestamp) -> timestamp < now - CONNECT_RATE_LIMIT_WINDOW);
        lastConnectTimes.add(now);
        return lastConnectTimes.size() > CONNECT_RATE_LIMIT_COUNT;
    }
}
