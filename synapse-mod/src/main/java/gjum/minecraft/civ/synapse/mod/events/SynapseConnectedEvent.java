package gjum.minecraft.civ.synapse.mod.events;

import gjum.minecraft.civ.synapse.mod.network.Client;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record SynapseConnectedEvent(
    @NotNull Client connection
) implements EventBus.ConnectionEvent {
    public SynapseConnectedEvent {
        Objects.requireNonNull(connection);
    }

    @Override
    public @NotNull Client getConnection() {
        return connection();
    }
}
