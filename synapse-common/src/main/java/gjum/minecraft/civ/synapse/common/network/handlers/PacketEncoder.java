package gjum.minecraft.civ.synapse.common.network.handlers;

import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.io.DataOutput;
import org.jetbrains.annotations.NotNull;

public abstract class PacketEncoder extends MessageToByteEncoder<Packet> {
    @Override
    protected final void encode(
        final @NotNull ChannelHandlerContext ctx,
        final @NotNull Packet packet,
        final @NotNull ByteBuf buf
    ) throws Exception {
        final DataOutput out = new ByteBufOutputStream(buf);
        out.writeByte(getPacketId(packet));
        packet.encode(out);
    }

    protected abstract byte getPacketId(
        @NotNull Packet packet
    ) throws Exception;
}
