package gjum.minecraft.civ.synapse.server.states;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import gjum.minecraft.civ.synapse.common.NameAndUuid;
import gjum.minecraft.civ.synapse.common.Util;
import gjum.minecraft.civ.synapse.common.network.CryptoTransformer;
import gjum.minecraft.civ.synapse.common.network.handlers.PacketDecrypter;
import gjum.minecraft.civ.synapse.common.network.handlers.PacketEncrypter;
import gjum.minecraft.civ.synapse.common.network.packets.UnexpectedPacketException;
import gjum.minecraft.civ.synapse.common.network.packets.clientbound.ClientboundEncryptionRequest;
import gjum.minecraft.civ.synapse.common.network.packets.clientbound.ClientboundIdentityRequest;
import gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundBeginHandshake;
import gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundEncryptionResponse;
import gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundIdentityResponse;
import gjum.minecraft.civ.synapse.common.network.states.ConnectionState;
import gjum.minecraft.civ.synapse.server.ClientSession;
import gjum.minecraft.civ.synapse.server.Server;
import io.netty.util.Attribute;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.spec.SecretKeySpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServerConnectionState implements ConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerConnectionState.class);

    // ============================================================
    // Handshake
    // ============================================================

    private static final KeyPair KEY_PAIR; static {
        final KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
        }
        catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("Something impossible just happened! RSA should exist!", e);
        }
        generator.initialize(1024);
        KEY_PAIR = generator.genKeyPair();
    }
    private static final byte[] PUBLIC_KEY_BYTES = KEY_PAIR.getPublic().getEncoded();

    /** Awaiting {@link gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundBeginHandshake} */
    private record AwaitingHandshake() implements ConnectionState {

    }

    public static void handleConnected(
        final @NotNull ClientSession client
    ) {
        client.channel.attr(ServerConnectionState.KEY).set(new AwaitingHandshake());
    }

    public static void handleBeginHandshake(
        final @NotNull ClientSession client,
        final @NotNull ServerboundBeginHandshake packet
    ) throws Exception {
        final Attribute<ConnectionState> attr = client.channel.attr(ServerConnectionState.KEY);
        if (!(attr.getAndSet(null) instanceof AwaitingHandshake)) {
            throw new UnexpectedPacketException(packet);
        }

        final var verifyToken = new byte[4];
        ThreadLocalRandom.current().nextBytes(verifyToken);

        attr.set(new AwaitingEncryptionResponse(
            verifyToken
        ));

        client.send(new ClientboundEncryptionRequest(
            PUBLIC_KEY_BYTES,
            verifyToken
        ));
    }

    // ============================================================
    // Encryption Response
    // ============================================================

    private static final CryptoTransformer DECRYPT; static {
        try {
            DECRYPT = new CryptoTransformer(KEY_PAIR.getPrivate(), CryptoTransformer.Type.DECRYPT);
        }
        catch (final InvalidKeyException e) {
            throw new IllegalStateException("Could not setup decryption cipher!", e);
        }
    }

    /** Awaiting {@link gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundEncryptionResponse} */
    private record AwaitingEncryptionResponse(
        byte @NotNull [] verifyToken
    ) implements ConnectionState {
        public AwaitingEncryptionResponse {
            Objects.requireNonNull(verifyToken);
        }
    }

    public static void handleEncryptionResponse(
        final @NotNull ClientSession client,
        final @NotNull ServerboundEncryptionResponse packet
    ) throws Exception {
        final Attribute<ConnectionState> attr = client.channel.attr(ServerConnectionState.KEY);
        if (!(attr.getAndSet(null) instanceof final AwaitingEncryptionResponse state)) {
            throw new UnexpectedPacketException(packet);
        }

        final byte[] decryptedVerifyToken = DECRYPT.transform(packet.verifyToken());
        if (!Arrays.equals(state.verifyToken(), decryptedVerifyToken)) {
            client.kick(
                "verifyToken does not match!",
                "Incorrect verification token!"
            );
            return;
        }

        final byte[] sharedSecret = DECRYPT.transform(packet.sharedSecret());

        final var key = new SecretKeySpec(sharedSecret, "AES");
        client.channel.pipeline()
            .addFirst("encrypt", new PacketEncrypter(key))
            .addFirst("decrypt", new PacketDecrypter(key));

        attr.set(new AwaitingIdentityResponse(
            sharedSecret
        ));

        client.send(new ClientboundIdentityRequest(
            sharedSecret
        ));
    }

    // ============================================================
    // Identity Response
    // ============================================================

    /** Awaiting {@link gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundIdentityResponse} */
    private record AwaitingIdentityResponse(
        byte @NotNull [] sharedSecret
    ) implements ConnectionState {
        public AwaitingIdentityResponse {
            Objects.requireNonNull(sharedSecret);
        }
    }

    public static void handleIdentityResponse(
        final @NotNull ClientSession client,
        final @NotNull ServerboundIdentityResponse packet
    ) throws Exception {
        final Attribute<ConnectionState> attr = client.channel.attr(ServerConnectionState.KEY);
        if (!(attr.getAndSet(null) instanceof final AwaitingIdentityResponse state)) {
            throw new UnexpectedPacketException(packet);
        }

        final NameAndUuid account;
        if (packet.authenticated()) {
            account = attemptAuthentication(
                state.sharedSecret(),
                packet.mojangUsername()
            );
            if (account == null) {
                client.kick(
                    "could not authenticate!",
                    "You are not authenticated!"
                );
                return;
            }
            if (!Objects.equals(account.uuid(), packet.uuid())) {
                client.kick(
                    "claimed UUID of [" + packet.uuid() + "] did not match auth UUID of [" + account.uuid() + "]",
                    "You are not who you claim to be! \uD83E\uDD28"
                );
                return;
            }
        }
        else if (Server.REQUIRES_AUTH) {
            client.kick(
                "Did not authenticate when required!",
                "This Synapse server requires that you be authenticated!"
            );
            return;
        }
        else {
            account = new NameAndUuid(
                packet.mojangUsername(),
                packet.uuid()
            );
        }

        if (!Server.GAME_ADDRESS.equals(packet.gameAddress())) {
            client.kick(
                "game address of [" + packet.gameAddress() + "] did not match supported address of [" + Server.GAME_ADDRESS + "]",
                "This Synapse server isn't for " + packet.gameAddress()
            );
            return;
        }

        attr.set(new ConnectionState.GreenCard(
            packet.namelayerUsername(),
            account.name(),
            account.uuid()
        ));
    }

    private static final Object AUTH_LOCK = new Object();
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final Gson GSON = new Gson();

    private static @Nullable NameAndUuid attemptAuthentication(
        final byte @NotNull [] sharedSecret,
        final @NotNull String claimedUsername
    ) throws IOException {
        final String sha; {
            final MessageDigest digest = Util.sha1();
            digest.update(sharedSecret);
            digest.update(PUBLIC_KEY_BYTES);
            sha = HexFormat.of().formatHex(digest.digest());
        }
        synchronized (AUTH_LOCK) {
            try (final Response response = HTTP_CLIENT.newCall(new Request.Builder().url("https://sessionserver.mojang.com/session/minecraft/hasJoined?username=" + claimedUsername + "&serverId=" + sha).build()).execute()) {
                if (response.code() != 200) {
                    return null;
                }
                final ResponseBody body = response.body();
                if (body == null) {
                    return null;
                }
                final JsonObject json = GSON.fromJson(body.string(), JsonObject.class);
                return new NameAndUuid(
                    json.get("name").getAsString(),
                    UUID.fromString(Util.addDashesToUuid(json.get("id").getAsString()))
                );
            }
        }
    }
}
