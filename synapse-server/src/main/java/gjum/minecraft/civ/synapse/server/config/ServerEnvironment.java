package gjum.minecraft.civ.synapse.server.config;

import com.google.common.net.HostAndPort;
import gjum.minecraft.civ.synapse.common.configs.StringParsing;
import java.io.File;
import java.util.concurrent.TimeUnit;

public final class ServerEnvironment {
    public static final int PORT = StringParsing.parseInt(
        System.getenv("SYNAPSE_PORT"),
        22001
    );

    public static final String GAME_ADDRESS = HostAndPort.fromString(StringParsing.parse(
        System.getenv("SYNAPSE_GAME_ADDRESS"),
        "play.civmc.net"
    )).withDefaultPort(25565).toString();

    public static final boolean REQUIRES_AUTH = StringParsing.parseBoolean(
        System.getenv("SYNAPSE_REQUIRES_AUTH"),
        true
    );

    public static final long CONNECT_RATE_LIMIT_WINDOW = TimeUnit.MINUTES.toMillis(StringParsing.parseLong(
        System.getenv("SYNAPSE_CONNECT_RATE_LIMIT_WINDOW"),
        1 // minute
    ));

    public static final int CONNECT_RATE_LIMIT_COUNT = StringParsing.parseInt(
        System.getenv("SYNAPSE_CONNECT_RATE_LIMIT_COUNT"),
        7 // 7 connections over the past rateLimitWindow
    );

    public static final File UUIDS_PATH = new File(StringParsing.parse(
        System.getenv("SYNAPSE_UUIDS_PATH"),
        "uuids.tsv"
    ));

    public static final File USERS_PATH = new File(StringParsing.parse(
        System.getenv("SYNAPSE_USERS_PATH"),
        "users.tsv"
    ));

    public static final File ADMINS_PATH = new File(StringParsing.parse(
        System.getenv("SYNAPSE_ADMINS_PATH"),
        "admins.tsv"
    ));
}
