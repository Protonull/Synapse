package gjum.minecraft.civ.synapse.common.network.packets;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public final class PacketHelpers {
    public static @NotNull ChannelHandler @NotNull [] generatePacketLengthPrefixHandlers() {
        return new ChannelHandler[] {
            new LengthFieldPrepender(Short.BYTES),
            new LengthFieldBasedFrameDecoder(1 << Short.SIZE, 0, Short.BYTES, 0, Short.BYTES, true),
        };
    }

    public static byte @NotNull [] readBytes(
        final @NotNull DataInput in,
        final @NotNull LengthPrefix prefix
    ) throws IOException {
        final var bytes = new byte[switch (prefix) {
            case i8 -> in.readByte();
            case i16 -> in.readShort();
            case i32 -> in.readInt();
        }];
        in.readFully(bytes);
        return bytes;
    }

    public static void writeBytes(
        final @NotNull DataOutput out,
        final @NotNull LengthPrefix prefix,
        final byte @NotNull [] bytes
    ) throws IOException {
        switch (prefix) {
            case i8 -> out.writeByte(bytes.length);
            case i16 -> out.writeShort(bytes.length);
            case i32 -> out.writeInt(bytes.length);
        }
        out.write(bytes);
    }

    public static @NotNull UUID readUuid(
        final @NotNull DataInput in
    ) throws IOException {
        return new UUID(
            in.readLong(),
            in.readLong()
        );
    }

    public static void writeUuid(
        final @NotNull DataOutput out,
        final @NotNull UUID value
    ) throws IOException {
        out.writeLong(value.getMostSignificantBits());
        out.writeLong(value.getLeastSignificantBits());
    }
}
