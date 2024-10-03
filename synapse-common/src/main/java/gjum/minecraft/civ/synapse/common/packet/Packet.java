package gjum.minecraft.civ.synapse.common.packet;

import static gjum.minecraft.civ.synapse.common.Util.nonNullOr;

import gjum.minecraft.civ.synapse.common.Pos;
import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Packet {
    public abstract void write(ByteBuf buf);

    @NotNull
    protected static String readString(@NotNull ByteBuf in) {
        return nonNullOr(readOptionalString(in), "");
    }

    @Nullable
    protected static String readOptionalString(@NotNull ByteBuf in) {
        final int length = in.readInt();
        if (length <= 0) return null;
        final byte[] bytes = new byte[length];
        in.readBytes(bytes);
        return new String(bytes);
    }

    protected static void writeOptionalString(@NotNull ByteBuf out, @Nullable String string) {
        if (string == null || string.isEmpty()) {
            out.writeInt(0);
            return;
        }
        final byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }

    @Nullable
    protected static Pos readOptionalPos(@NotNull ByteBuf in) {
        Pos pos = null;
        if (in.readBoolean()) {
            pos = new Pos(
                    in.readInt(),
                    in.readInt(),
                    in.readInt());
        }
        return pos;
    }

    protected static void writeOptionalPos(@NotNull ByteBuf out, @Nullable Pos pos) {
        out.writeBoolean(pos != null);
        if (pos != null) {
            out.writeInt(pos.x);
            out.writeInt(pos.y);
            out.writeInt(pos.z);
        }
    }

    protected static byte[] readByteArray(ByteBuf in) {
        final int length = in.readInt();
        final byte[] array = new byte[length];
        in.readBytes(array);
        return array;
    }

    protected static void writeByteArray(ByteBuf out, byte[] array) {
        out.writeInt(array.length);
        out.writeBytes(array);
    }

    @NotNull
    protected static PublicKey readKey(ByteBuf in) {
        try {
            final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(readByteArray(in));
            final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
