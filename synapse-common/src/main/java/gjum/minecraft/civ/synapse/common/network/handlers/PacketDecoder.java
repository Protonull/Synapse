package gjum.minecraft.civ.synapse.common.network.handlers;

import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import java.io.DataInput;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public abstract class PacketDecoder extends ReplayingDecoder<Void> {
    @Override
    protected final void decode(
        final @NotNull ChannelHandlerContext ctx,
        final @NotNull ByteBuf buf,
        final @NotNull List<@NotNull Object> out
    ) throws Exception {
        final DataInput in = new ByteBufInputStream(buf);
        out.add(decodePacket(in, in.readByte()));
    }

    /**
     * Decodes and constructs a new packet instance. You are expected to handle and throw for any unsupported or
     * unexpected packets!
     */
    protected abstract @NotNull Packet decodePacket(
        @NotNull DataInput in,
        byte packetId
    ) throws Exception;
}
