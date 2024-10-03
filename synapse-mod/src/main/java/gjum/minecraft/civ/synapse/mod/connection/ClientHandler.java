package gjum.minecraft.civ.synapse.mod.connection;

import static gjum.minecraft.civ.synapse.mod.LiteModSynapse.MOD_NAME;

import com.mojang.authlib.exceptions.AuthenticationException;
import gjum.minecraft.civ.synapse.common.encryption.DecryptStage;
import gjum.minecraft.civ.synapse.common.encryption.EncryptStage;
import gjum.minecraft.civ.synapse.common.packet.JsonPacket;
import gjum.minecraft.civ.synapse.common.packet.client.CEncryptionResponse;
import gjum.minecraft.civ.synapse.common.packet.server.SEncryptionRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ConnectException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

public class ClientHandler extends ChannelInboundHandlerAdapter {
    private Client client;

    public ClientHandler(Client client) {
        this.client = client;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object packet) {
        if (packet instanceof SEncryptionRequest) {
            try {
                setupEncryption(ctx, (SEncryptionRequest) packet);
            } catch (AuthenticationException e) {
                client.logger.warn("Auth error: " + e.getMessage(), e);
            }
        } else if (packet instanceof JsonPacket) {
            client.handleJsonPacket((JsonPacket) packet);
        }
    }

    private void setupEncryption(ChannelHandlerContext ctx, SEncryptionRequest packet) throws AuthenticationException {
        PublicKey key = packet.key;

        byte[] sharedSecret = new byte[16];
        ThreadLocalRandom.current().nextBytes(sharedSecret);

        String sha;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(sharedSecret);
            digest.update(key.getEncoded());
            sha = new BigInteger(digest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        User session = Minecraft.getInstance().getUser();
        Minecraft.getInstance().getMinecraftSessionService().joinServer(session.getProfileId(), session.getAccessToken(), sha);

        try {
            ctx.channel().writeAndFlush(new CEncryptionResponse(
                    encrypt(key, sharedSecret),
                    encrypt(key, packet.verifyToken)));
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException e) {
            client.disconnect();
            throw new RuntimeException(e);
        }

        SecretKey secretKey = new SecretKeySpec(sharedSecret, "AES");
        ctx.pipeline()
                .addFirst("encrypt", new EncryptStage(secretKey))
                .addFirst("decrypt", new DecryptStage(secretKey));

        client.handleEncryptionSuccess(packet.message);
    }

    private static byte[] encrypt(PublicKey key, byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
        final Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException && "Connection reset by peer".equals(cause.getMessage())) return;
        if (cause instanceof ConnectException && cause.getMessage().startsWith("Connection refused: ")) return;

        client.logger.info("[" + MOD_NAME + "] Network Error: " + cause);
        cause.printStackTrace();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        client.handleDisconnect(new RuntimeException("Channel inactive"));
        super.channelInactive(ctx);
    }
}
