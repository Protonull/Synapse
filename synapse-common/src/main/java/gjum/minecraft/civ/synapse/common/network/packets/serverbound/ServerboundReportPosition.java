package gjum.minecraft.civ.synapse.common.network.packets.serverbound;

import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import java.io.DataInput;
import java.io.DataOutput;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Should only be sent after having received a {@link gjum.minecraft.civ.synapse.common.network.packets.clientbound.ClientboundWelcome}
 * from the server. The client should refrain from sending this packet if none of the information has changed since the
 * last time it was sent.
 */
public record ServerboundReportPosition(
    @NotNull String dimensionKey,
    int x,
    int y,
    int z
) implements Packet {
    public ServerboundReportPosition {
        Objects.requireNonNull(dimensionKey);
    }

    @Override
    public void encode(
        final @NotNull DataOutput out
    ) throws Exception {
        out.writeUTF(dimensionKey());
        out.writeInt(x());
        out.writeInt(y());
        out.writeInt(z());
    }

    public static @NotNull ServerboundReportPosition decode(
        final @NotNull DataInput in
    ) throws Exception {
        return new ServerboundReportPosition(
            in.readUTF(),
            in.readInt(),
            in.readInt(),
            in.readInt()
        );
    }
}
