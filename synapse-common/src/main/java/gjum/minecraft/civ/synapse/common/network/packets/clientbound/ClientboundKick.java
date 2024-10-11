package gjum.minecraft.civ.synapse.common.network.packets.clientbound;

import com.google.common.base.Strings;
import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import java.io.DataInput;
import java.io.DataOutput;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record ClientboundKick(
    @NotNull String reason
) implements Packet {
    public ClientboundKick {
        reason = Objects.requireNonNullElse(
            Strings.emptyToNull(Objects.requireNonNull(reason)),
            "<no reason given>"
        );
    }

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
