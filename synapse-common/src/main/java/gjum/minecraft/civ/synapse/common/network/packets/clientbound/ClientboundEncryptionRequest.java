package gjum.minecraft.civ.synapse.common.network.packets.clientbound;

import com.google.common.base.Strings;
import gjum.minecraft.civ.synapse.common.network.packets.LengthPrefix;
import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import gjum.minecraft.civ.synapse.common.network.packets.PacketHelpers;
import java.io.DataInput;
import java.io.DataOutput;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ClientboundEncryptionRequest(
    @NotNull PublicKey key,
    byte @NotNull [] verifyToken,
    @Nullable String message
) implements Packet {
    public ClientboundEncryptionRequest {
        Objects.requireNonNull(key);
        Objects.requireNonNull(verifyToken);
    }

    @Override
    public void encode(
        final @NotNull DataOutput out
    ) throws Exception {
        PacketHelpers.writeBytes(out, LengthPrefix.i32, key().getEncoded());
        PacketHelpers.writeBytes(out, LengthPrefix.i32, verifyToken());
        out.writeUTF(Objects.requireNonNullElse(message(), ""));
    }

    public static @NotNull ClientboundEncryptionRequest decode(
        final @NotNull DataInput in
    ) throws Exception {
        return new ClientboundEncryptionRequest(
            KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(PacketHelpers.readBytes(in, in.readInt()))),
            PacketHelpers.readBytes(in, in.readInt()),
            Strings.emptyToNull(in.readUTF())
        );
    }
}
