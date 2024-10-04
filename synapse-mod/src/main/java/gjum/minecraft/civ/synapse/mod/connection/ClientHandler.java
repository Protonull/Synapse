package gjum.minecraft.civ.synapse.mod.connection;

import com.mojang.authlib.exceptions.AuthenticationException;
import gjum.minecraft.civ.synapse.common.Util;
import gjum.minecraft.civ.synapse.common.network.handlers.PacketDecrypter;
import gjum.minecraft.civ.synapse.common.network.handlers.PacketEncrypter;
import gjum.minecraft.civ.synapse.common.network.packets.JsonPacket;
import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import gjum.minecraft.civ.synapse.common.network.packets.clientbound.ClientboundEncryptionRequest;
import gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundEncryptionResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.io.IOException;
import java.net.ConnectException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import org.jetbrains.annotations.NotNull;

public class ClientHandler extends ChannelInboundHandlerAdapter {
    private final Client client;

    public ClientHandler(
        final @NotNull Client client
    ) {
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public void channelRead(
        final @NotNull ChannelHandlerContext ctx,
        final @NotNull Object packet
    ) {
        switch (packet) {
            case final ClientboundEncryptionRequest encryptionRequest -> {
                setupEncryption(ctx, encryptionRequest);
            }
            case final JsonPacket json -> {
                this.client.handleJsonPacket(json);
            }
            default -> {} // Do nothing
        }
    }

    private void setupEncryption(
        final @NotNull ChannelHandlerContext ctx,
        final @NotNull ClientboundEncryptionRequest packet
    ) {
        final var sharedSecret = new byte[16];
        ThreadLocalRandom.current().nextBytes(sharedSecret);

        final String sha; {
            final MessageDigest md = Util.sha1();
            md.update(sharedSecret);
            md.update(packet.key().getEncoded());
            sha = HexFormat.of().formatHex(md.digest());
        }

        try {
            final User session = Minecraft.getInstance().getUser();
            Minecraft.getInstance().getMinecraftSessionService().joinServer(
                session.getProfileId(),
                session.getAccessToken(),
                sha
            );
        }
        catch (final AuthenticationException e) {
            this.client.logger.warn("Auth error: {}", e.getMessage(), e);
            ctx.close();
            return;
        }

        final Packet encryptionResponse;
        try {
            final Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, packet.key());
            encryptionResponse = new ServerboundEncryptionResponse(
                cipher.doFinal(sharedSecret),
                cipher.doFinal(packet.verifyToken())
            );
        }
        catch (final InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException e) {
            this.client.logger.warn("Could not init cipher with: {}", packet.key(), e);
            ctx.close();
            return;
        }
        catch (final IllegalBlockSizeException | BadPaddingException e) {
            this.client.logger.warn("Could not encrypt response packet!", e);
            ctx.close();
            return;
        }
        ctx.channel().writeAndFlush(encryptionResponse);

        final SecretKey secretKey = new SecretKeySpec(sharedSecret, "AES");
        ctx.pipeline()
            .addFirst("encrypt", new PacketEncrypter(secretKey))
            .addFirst("decrypt", new PacketDecrypter(secretKey));

        this.client.handleEncryptionSuccess(packet.message());
    }

    @Override
    public void exceptionCaught(
        final @NotNull ChannelHandlerContext ctx,
        final @NotNull Throwable cause
    ) {
        if (cause instanceof IOException && "Connection reset by peer".equals(cause.getMessage())) return;
        if (cause instanceof ConnectException && cause.getMessage().startsWith("Connection refused: ")) return;
        this.client.logger.info("Network Error: {}", String.valueOf(cause), cause);
    }

    @Override
    public void channelInactive(
        final @NotNull ChannelHandlerContext ctx
    ) throws Exception {
        this.client.handleDisconnect(new RuntimeException("Channel inactive"));
        super.channelInactive(ctx);
    }
}
