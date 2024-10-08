package gjum.minecraft.civ.synapse.common.network.packets;

import java.io.DataInput;
import java.io.DataOutput;
import org.jetbrains.annotations.NotNull;

public interface Packet {
    // Handshake
    byte SERVERBOUND_BEGIN_HANDSHAKE = 0;
    byte CLIENTBOUND_ENCRYPTION_REQUEST = 1;
    byte SERVERBOUND_ENCRYPTION_RESPONSE = 2;
    byte CLIENTBOUND_IDENTITY_REQUEST = 3;
    byte SERVERBOUND_IDENTITY_RESPONSE = 4;

    void encode(
        @NotNull DataOutput out
    ) throws Exception;

    static @NotNull Packet decode(
        final @NotNull DataInput in
    ) throws Exception {
        throw new RuntimeException("not implemented");
    }
}
