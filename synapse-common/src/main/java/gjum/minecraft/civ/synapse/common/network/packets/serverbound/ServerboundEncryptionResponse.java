package gjum.minecraft.civ.synapse.common.network.packets.serverbound;

import gjum.minecraft.civ.synapse.common.network.packets.LengthPrefix;
import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import gjum.minecraft.civ.synapse.common.network.packets.PacketHelpers;
import java.io.DataInput;
import java.io.DataOutput;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record ServerboundEncryptionResponse(
    byte @NotNull [] sharedSecret,
    byte @NotNull [] verifyToken
) implements Packet {
    public ServerboundEncryptionResponse {
        Objects.requireNonNull(sharedSecret);
        Objects.requireNonNull(verifyToken);
    }

    @Override
    public void encode(
        final @NotNull DataOutput out
    ) throws Exception {
        PacketHelpers.writeBytes(out, LengthPrefix.i32, sharedSecret());
        PacketHelpers.writeBytes(out, LengthPrefix.i32, verifyToken());
    }

    public static @NotNull ServerboundEncryptionResponse decode(
        final @NotNull DataInput in
    ) throws Exception {
        return new ServerboundEncryptionResponse(
            PacketHelpers.readBytes(in, in.readInt()),
            PacketHelpers.readBytes(in, in.readInt())
        );
    }
}
