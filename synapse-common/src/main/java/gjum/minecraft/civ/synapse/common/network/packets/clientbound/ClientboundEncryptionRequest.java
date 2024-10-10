package gjum.minecraft.civ.synapse.common.network.packets.clientbound;

import gjum.minecraft.civ.synapse.common.network.packets.LengthPrefix;
import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import gjum.minecraft.civ.synapse.common.network.packets.PacketHelpers;
import java.io.DataInput;
import java.io.DataOutput;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Prev: {@link gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundBeginHandshake}
 * Next: {@link gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundEncryptionResponse}
 */
public record ClientboundEncryptionRequest(
    byte @NotNull [] publicKey,
    byte @NotNull [] verifyToken
) implements Packet {
    public ClientboundEncryptionRequest {
        Objects.requireNonNull(publicKey);
        Objects.requireNonNull(verifyToken);
    }

    @Override
    public void encode(
        final @NotNull DataOutput out
    ) throws Exception {
        PacketHelpers.writeBytes(out, LengthPrefix.i16, publicKey());
        PacketHelpers.writeBytes(out, LengthPrefix.i16, verifyToken());
    }

    public static @NotNull ClientboundEncryptionRequest decode(
        final @NotNull DataInput in
    ) throws Exception {
        return new ClientboundEncryptionRequest(
            PacketHelpers.readBytes(in, LengthPrefix.i16),
            PacketHelpers.readBytes(in, LengthPrefix.i16)
        );
    }
}
