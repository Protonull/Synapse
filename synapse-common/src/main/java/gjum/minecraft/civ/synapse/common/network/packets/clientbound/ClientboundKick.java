package gjum.minecraft.civ.synapse.common.network.packets.clientbound;

import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import java.io.DataInput;
import java.io.DataOutput;
import org.jetbrains.annotations.NotNull;

public record ClientboundKick(
    @NotNull String reason
) implements Packet {
    @Override
    public void encode(
        final @NotNull DataOutput out
    ) throws Exception {
        out.writeUTF(reason());
    }

    public static @NotNull ClientboundKick decode(
        final @NotNull DataInput in
    ) throws Exception {
        return new ClientboundKick(
            in.readUTF()
        );
    }
}
