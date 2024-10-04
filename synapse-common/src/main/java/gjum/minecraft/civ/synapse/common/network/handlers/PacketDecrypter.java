package gjum.minecraft.civ.synapse.common.network.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import org.jetbrains.annotations.NotNull;

public final class PacketDecrypter extends MessageToMessageDecoder<ByteBuf> {
    private final Cipher cipher;

    public PacketDecrypter(
        final @NotNull Key key
    ) {
        try {
            this.cipher = Cipher.getInstance("AES/CFB8/NoPadding");
            this.cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(key.getEncoded()));
        }
        catch (final GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void decode(
        final @NotNull ChannelHandlerContext ctx,
        final @NotNull ByteBuf in,
        final @NotNull List<@NotNull Object> out
    ) throws ShortBufferException {
        final var data = new byte[in.readableBytes()];
        in.readBytes(data);
        final var resultBytes = new byte[this.cipher.getOutputSize(data.length)];
        final int numberOfBytesDecrypted = this.cipher.update(data, 0, data.length, resultBytes);
        out.add(Unpooled.wrappedBuffer(resultBytes, 0, numberOfBytesDecrypted));
    }
}
