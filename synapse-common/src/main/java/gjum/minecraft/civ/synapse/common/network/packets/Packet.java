package gjum.minecraft.civ.synapse.common.network.packets;

import java.io.DataInput;
import java.io.DataOutput;
import org.jetbrains.annotations.NotNull;

public interface Packet {
    byte CLIENTBOUND_KICK = 0;

    // Handshake
    byte SERVERBOUND_BEGIN_HANDSHAKE = 1;
    byte CLIENTBOUND_ENCRYPTION_REQUEST = 2;
    byte SERVERBOUND_ENCRYPTION_RESPONSE = 3;
    byte CLIENTBOUND_IDENTITY_REQUEST = 4;
    byte SERVERBOUND_IDENTITY_RESPONSE = 5;
    byte CLIENTBOUND_WELCOME = 6;

    void encode(
        @NotNull DataOutput out
    ) throws Exception;

    static @NotNull Packet decode(
        final @NotNull DataInput in
    ) throws Exception {
        throw new RuntimeException("not implemented");
    }
}
