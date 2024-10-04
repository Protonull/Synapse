package gjum.minecraft.civ.synapse.common.network.packets.serverbound;

import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import java.io.DataInput;
import java.io.DataOutput;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record ServerboundHandshake(
    @NotNull String synapseVersion,
    @NotNull String username,
    @NotNull String gameAddress
) implements Packet {
    public ServerboundHandshake {
        Objects.requireNonNull(synapseVersion);
        Objects.requireNonNull(username);
        Objects.requireNonNull(gameAddress);
    }

    @Override
    public void encode(
        final @NotNull DataOutput out
    ) throws Exception {
        out.writeUTF(this.synapseVersion);
        out.writeUTF(this.username);
        out.writeUTF(this.gameAddress);
    }

    public static @NotNull ServerboundHandshake decode(
        final @NotNull DataInput in
    ) throws Exception {
        return new ServerboundHandshake(
            in.readUTF(),
            in.readUTF(),
            in.readUTF()
        );
    }
}
