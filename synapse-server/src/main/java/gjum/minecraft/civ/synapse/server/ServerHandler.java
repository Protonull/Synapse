package gjum.minecraft.civ.synapse.server;

import static gjum.minecraft.civ.synapse.common.Util.addDashesToUuid;
import static gjum.minecraft.civ.synapse.common.Util.bytesToHex;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import gjum.minecraft.civ.synapse.common.Util;
import gjum.minecraft.civ.synapse.common.network.handlers.PacketDecrypter;
import gjum.minecraft.civ.synapse.common.network.handlers.PacketEncrypter;
import gjum.minecraft.civ.synapse.common.network.packets.JsonPacket;
import gjum.minecraft.civ.synapse.common.network.packets.clientbound.ClientboundEncryptionRequest;
import gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundEncryptionResponse;
import gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundHandshake;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderException;
import io.netty.util.internal.ThreadLocalRandom;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServerHandler extends ChannelInboundHandlerAdapter {
    private static final Gson gson = new Gson();

    private final Server server;
    private final OkHttpClient httpClient = new OkHttpClient();

    public ServerHandler(
        final @NotNull Server server
    ) {
        this.server = Objects.requireNonNull(server);
    }

    @Override
    public void channelActive(
        final @NotNull ChannelHandlerContext ctx
    ) {
        this.server.getOrCreateClient(ctx.channel());
    }

    @Override
    public void exceptionCaught(
        final @NotNull ChannelHandlerContext ctx,
        final @NotNull Throwable cause
    ) {
        if (cause instanceof IOException && "Connection reset by peer".equals(cause.getMessage())) {
            return;
        }

        cause.printStackTrace();

        if (cause instanceof DecoderException) {
            ClientSession client = server.getOrCreateClient(ctx.channel());
            client.addDisconnectReason("DecoderException: " + cause);
            ctx.channel().disconnect();
        }
    }

    @Override
    public void channelRead(
        final @NotNull ChannelHandlerContext ctx,
        final @NotNull Object packet
    ) {
        final ClientSession client = this.server.getOrCreateClient(ctx.channel());
        if (!client.isHandshaked()) {
            if (!(packet instanceof final ServerboundHandshake handshake)) {
                this.server.kick(client, "Expected Handshake, got " + packet);
                return;
            }

            client.synapseVersion = handshake.synapseVersion();
            client.claimedUsername = handshake.username();

            if (!isValidUsername(handshake.username())) {
                this.server.kick(client, "Handshake username contains illegal characters: '" + handshake.username() + "'");
                return;
            }

            ThreadLocalRandom.current().nextBytes(client.verifyToken = new byte[4]);
            final String infoMessage = this.server.handleClientHandshaking(client, handshake);

            client.send(new ClientboundEncryptionRequest(
                this.server.getPublicKey(),
                client.verifyToken,
                infoMessage
            ));
            return;
        }
        if (!client.isAuthenticated()) {
            if (!(packet instanceof final ServerboundEncryptionResponse encryptionResponse)) {
                Server.log(client, Level.WARNING, "Expected encryption response, received " + packet);
                ctx.disconnect();
                return;
            }

            final byte[] sharedSecret;
            final byte[] verifyToken;
            try {
                sharedSecret = this.server.decrypt(encryptionResponse.sharedSecret());
                verifyToken = this.server.decrypt(encryptionResponse.verifyToken());
            }
            catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                Server.log(client, Level.WARNING, "Could not decrypt shared secret/verify token: " + e);
                ctx.disconnect();
                return;
            }
            if (!Arrays.equals(verifyToken, client.verifyToken)) {
                Server.log(client, Level.WARNING, "Verify token invalid. Got: '" + bytesToHex(verifyToken)
                        + "' expected: '" + bytesToHex(client.verifyToken) + "'");
                ctx.disconnect();
                return;
            }

            final String sha; {
                final MessageDigest digest = Util.sha1();
                digest.update(sharedSecret);
                digest.update(server.getPublicKey().getEncoded());
                sha = HexFormat.of().formatHex(digest.digest());
            }

            final Request hasJoined = new Request.Builder()
                    .url("https://sessionserver.mojang.com/session/minecraft/hasJoined?username=" + client.claimedUsername + "&serverId=" + sha)
                    .build();
            try (final Response response = this.httpClient.newCall(hasJoined).execute()) {
                if (response.code() != 200) {
                    Server.log(client, Level.WARNING, "Mojang response not OK. code: " + response.code());
                    ctx.disconnect();
                    return;
                }

                final JsonObject json = gson.fromJson(response.body().string(), JsonObject.class);
                final String mojangAccount = json.get("name").getAsString();
                final UUID mojangUuid = UUID.fromString(addDashesToUuid(json.get("id").getAsString()));
                if (!mojangAccount.equalsIgnoreCase(client.claimedUsername)) {
                    this.server.kick(client, "Username mismatch. Client: '" + client.claimedUsername + "' Mojang: '" + mojangAccount + "'");
                    return;
                }
                final String civRealmsAccount = this.server.uuidMapper.getUsernameByUuid(mojangUuid);
                client.setAccountInfo(mojangUuid, mojangAccount, civRealmsAccount);

                final SecretKey key = new SecretKeySpec(sharedSecret, "AES");
                ctx.pipeline()
                        .addFirst("encrypt", new PacketEncrypter(key))
                        .addFirst("decrypt", new PacketDecrypter(key));

                this.server.handleClientAuthenticated(client);
            }
            catch (IOException e) {
                Server.log(client, Level.WARNING, "Error while authenticating");
                e.printStackTrace();
                ctx.disconnect();
            }
            return;
        }
        if (packet instanceof final JsonPacket jsonPacket) {
            this.server.handleJsonPacket(client, jsonPacket);
            return;
        }
        Server.log(client, Level.WARNING, "Received unexpected packet " + packet);
    }

    private static boolean isValidUsername(@Nullable String name) {
        if (name == null || name.length() > 16 || name.isEmpty()) {
            return false;
        }
        for (char c : name.toCharArray()) {
            if (!('a' <= c && c <= 'z')
                    && !('A' <= c && c <= 'Z')
                    && !('0' <= c && c <= '9')
                    && c != '_') {
                return false;
            }
        }
        return true;
    }
}
