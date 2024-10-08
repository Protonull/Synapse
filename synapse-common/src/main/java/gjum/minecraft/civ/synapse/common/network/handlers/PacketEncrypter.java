package gjum.minecraft.civ.synapse.common.network.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import org.jetbrains.annotations.NotNull;

public final class PacketEncrypter extends MessageToByteEncoder<ByteBuf> {
    private final Cipher cipher;

    public PacketEncrypter(
        final @NotNull Key key
    ) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        this.cipher = Cipher.getInstance("AES/CFB8/NoPadding");
        this.cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(key.getEncoded()));
    }

    @Override
    protected void encode(
        final @NotNull ChannelHandlerContext ctx,
        final @NotNull ByteBuf in,
        final @NotNull ByteBuf out
    ) throws ShortBufferException {
        final var data = new byte[in.readableBytes()];
        in.readBytes(data);
        final var resultBytes = new byte[this.cipher.getOutputSize(data.length)];
        final int numberOfBytesEncrypted = this.cipher.update(data, 0, data.length, resultBytes);
        out.writeBytes(resultBytes, 0, numberOfBytesEncrypted);
    }
}
