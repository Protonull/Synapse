package gjum.minecraft.civ.synapse.common.network.packets.serverbound;

import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import java.io.DataInput;
import java.io.DataOutput;
import org.jetbrains.annotations.NotNull;

/**
 * The client should send this IMMEDIATELY after connecting to the server.
 *
 * Next: {@link gjum.minecraft.civ.synapse.common.network.packets.clientbound.ClientboundEncryptionRequest}
 */
public record ServerboundBeginHandshakePacket(
    int synapseProtocol
) implements Packet {
    @Override
    public void encode(
        final @NotNull DataOutput out
    ) throws Exception {
        out.writeInt(synapseProtocol());
    }

    public static @NotNull ServerboundBeginHandshakePacket decode(
        final @NotNull DataInput in
    ) throws Exception {
        return new ServerboundBeginHandshakePacket(
            in.readInt()
        );
    }
}
