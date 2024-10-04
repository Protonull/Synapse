package gjum.minecraft.civ.synapse.common.network.packets;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public final class PacketHelpers {
    public static @NotNull ChannelHandler @NotNull [] generatePacketLengthPrefixHandlers() {
        return new ChannelHandler[] {
            new LengthFieldPrepender(Short.BYTES),
            new LengthFieldBasedFrameDecoder(Short.MAX_VALUE, 0, Short.BYTES, 0, Short.BYTES),
        };
    }

    public static byte @NotNull [] readBytes(
        final @NotNull DataInput in,
        final @Range(from = 0, to = Integer.MAX_VALUE) int length
    ) throws IOException {
        final var bytes = new byte[length];
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
            case i64 -> out.writeLong(bytes.length);
        }
        out.write(bytes);
    }
}
