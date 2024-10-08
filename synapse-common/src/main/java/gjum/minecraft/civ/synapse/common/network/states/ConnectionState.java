package gjum.minecraft.civ.synapse.common.network.states;

import gjum.minecraft.civ.synapse.common.Util;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public interface ConnectionState {
    AttributeKey<ConnectionState> KEY = AttributeKey.valueOf(ConnectionState.class, "connection");

    record GreenCard(
        @NotNull String namelayerName,
        @NotNull String mojangName,
        @NotNull UUID uuid
    ) implements ConnectionState {
        public GreenCard {
            if (!Util.isValidMinecraftUsername(namelayerName)) {
                throw new IllegalArgumentException("Invalid NameLayer name: " + namelayerName);
            }
            if (!Util.isValidMinecraftUsername(mojangName)) {
                throw new IllegalArgumentException("Invalid Mojang name: " + namelayerName);
            }
            Objects.requireNonNull(uuid);
        }
    }

    static void requiresGreenCard(
        final @NotNull Channel channel
    ) {
        if (!(channel.attr(KEY).get() instanceof GreenCard)) {
            throw new IllegalStateException("Doesn't have a green card!");
        }
    }
}
