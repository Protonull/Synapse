package gjum.minecraft.civ.synapse.server;

import gjum.minecraft.civ.synapse.server.config.AccountsListConfig;
import gjum.minecraft.civ.synapse.server.config.ServerEnvironment;
import gjum.minecraft.civ.synapse.server.config.UuidsConfig;
import gjum.minecraft.civ.synapse.server.network.TcpServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Server {
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    public static final UuidsConfig UUIDS = new UuidsConfig();
    public static final AccountsListConfig USERS = new AccountsListConfig(UUIDS);
    public static final AccountsListConfig ADMINS = new AccountsListConfig(UUIDS);

    public static void main(
        final @NotNull String @NotNull [] args
    ) throws Throwable {
        // must be loaded first; others depend on it during loading
        UUIDS.load(ServerEnvironment.UUIDS_PATH);
        UUIDS.saveLater(null);

        USERS.load(ServerEnvironment.USERS_PATH);
        USERS.saveLater(null);

        ADMINS.load(ServerEnvironment.ADMINS_PATH);
        ADMINS.saveLater(null);

        TcpServer.start();
        LOGGER.info(
            "Starting server. PORT={}, GAME_ADDRESS={}, REQUIRES_AUTH={}",
            ServerEnvironment.PORT,
            ServerEnvironment.GAME_ADDRESS,
            ServerEnvironment.REQUIRES_AUTH
        );
    }
}
