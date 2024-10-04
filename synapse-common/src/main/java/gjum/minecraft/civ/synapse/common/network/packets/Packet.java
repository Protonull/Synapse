package gjum.minecraft.civ.synapse.common.network.packets;

import java.io.DataInput;
import java.io.DataOutput;
import org.jetbrains.annotations.NotNull;

public interface Packet {
    void encode(
        @NotNull DataOutput out
    ) throws Exception;

    static @NotNull Packet decode(
        final @NotNull DataInput in
    ) throws Exception {
        throw new RuntimeException("not implemented");
    }
}
