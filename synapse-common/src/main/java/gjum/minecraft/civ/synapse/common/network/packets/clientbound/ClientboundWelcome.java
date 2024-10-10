package gjum.minecraft.civ.synapse.common.network.packets.clientbound;

import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import java.io.DataInput;
import java.io.DataOutput;
import org.jetbrains.annotations.NotNull;

/**
 * This is used to tell the client that they've successfully connected.
 *
 * Prev: {@link gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundIdentityResponse}
 */
public record ClientboundWelcome() implements Packet {
    @Override
    public void encode(
        final @NotNull DataOutput out
    ) throws Exception {

    }

    public static @NotNull ClientboundWelcome decode(
        final @NotNull DataInput in
    ) throws Exception {
        return new ClientboundWelcome();
    }
}
