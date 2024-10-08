package gjum.minecraft.civ.synapse.common.network.packets.serverbound;

import gjum.minecraft.civ.synapse.common.network.packets.LengthPrefix;
import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import gjum.minecraft.civ.synapse.common.network.packets.PacketHelpers;
import java.io.DataInput;
import java.io.DataOutput;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Prev: {@link gjum.minecraft.civ.synapse.common.network.packets.clientbound.ClientboundEncryptionRequest}
 * Next: {@link gjum.minecraft.civ.synapse.common.network.packets.clientbound.ClientboundIdentityRequest}
 */
public record ServerboundEncryptionResponse(
    byte @NotNull [] verifyToken,
    byte @NotNull [] sharedSecret
) implements Packet {
    public ServerboundEncryptionResponse {
        Objects.requireNonNull(sharedSecret);
        Objects.requireNonNull(verifyToken);
    }

    @Override
    public void encode(
        final @NotNull DataOutput out
    ) throws Exception {
        PacketHelpers.writeBytes(out, LengthPrefix.i32, verifyToken());
        PacketHelpers.writeBytes(out, LengthPrefix.i32, sharedSecret());
    }

    public static @NotNull ServerboundEncryptionResponse decode(
        final @NotNull DataInput in
    ) throws Exception {
        return new ServerboundEncryptionResponse(
            PacketHelpers.readBytes(in, LengthPrefix.i32),
            PacketHelpers.readBytes(in, LengthPrefix.i32)
        );
    }
}
