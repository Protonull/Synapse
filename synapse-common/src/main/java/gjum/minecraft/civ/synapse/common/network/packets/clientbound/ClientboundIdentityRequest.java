package gjum.minecraft.civ.synapse.common.network.packets.clientbound;

import gjum.minecraft.civ.synapse.common.network.packets.LengthPrefix;
import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import gjum.minecraft.civ.synapse.common.network.packets.PacketHelpers;
import java.io.DataInput;
import java.io.DataOutput;
import org.jetbrains.annotations.NotNull;

/**
 * Prev: {@link gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundEncryptionResponse}
 * Next: {@link gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundIdentityResponse}
 */
public record ClientboundIdentityRequest(
    // This is only being sent to ensure that encryption is set up correctly
    byte @NotNull [] sharedSecret
) implements Packet {
    @Override
    public void encode(
        final @NotNull DataOutput out
    ) throws Exception {
        PacketHelpers.writeBytes(out, LengthPrefix.i32, sharedSecret());
    }

    public static @NotNull ClientboundIdentityRequest decode(
        final @NotNull DataInput in
    ) throws Exception {
        return new ClientboundIdentityRequest(
            PacketHelpers.readBytes(in, LengthPrefix.i32)
        );
    }
}
