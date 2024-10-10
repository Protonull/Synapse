package gjum.minecraft.civ.synapse.mod.network.states;

import com.google.common.net.HostAndPort;
import com.mojang.authlib.exceptions.AuthenticationException;
import gjum.minecraft.civ.synapse.common.Util;
import gjum.minecraft.civ.synapse.common.network.handlers.PacketDecrypter;
import gjum.minecraft.civ.synapse.common.network.handlers.PacketEncrypter;
import gjum.minecraft.civ.synapse.common.network.packets.UnexpectedPacketException;
import gjum.minecraft.civ.synapse.common.network.packets.clientbound.ClientboundEncryptionRequest;
import gjum.minecraft.civ.synapse.common.network.packets.clientbound.ClientboundIdentityRequest;
import gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundBeginHandshake;
import gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundEncryptionResponse;
import gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundIdentityResponse;
import gjum.minecraft.civ.synapse.common.network.states.ConnectionState;
import gjum.minecraft.civ.synapse.mod.network.Client;
import io.netty.util.Attribute;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientConnectionState implements ConnectionState {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientConnectionState.class);

    // ============================================================
    // Encryption Request
    // ============================================================

    private static final KeyFactory KEY_FACTORY; static {
        try {
            KEY_FACTORY = KeyFactory.getInstance("RSA");
        }
        catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("Something impossible happened! RSA is mandated!", e);
        }
    }

    /** Awaiting {@link gjum.minecraft.civ.synapse.common.network.packets.clientbound.ClientboundEncryptionRequest} */
    private record AwaitingEncryptionRequest() implements ConnectionState {

    }

    public static void handleConnected(
        final @NotNull Client client
    ) {
        client.channel.attr(ClientConnectionState.KEY).set(new AwaitingEncryptionRequest());
        client.send(new ServerboundBeginHandshake(
            0 // TODO: Make this value actually mean something
        ));
    }

    public static void handleEncryptionRequest(
        final @NotNull Client client,
        final @NotNull ClientboundEncryptionRequest packet
    ) throws Exception {
        final Attribute<ConnectionState> attr = client.channel.attr(ClientConnectionState.KEY);
        if (!(attr.getAndSet(null) instanceof AwaitingEncryptionRequest)) {
            throw new UnexpectedPacketException(packet);
        }
        final PublicKey publicKey = KEY_FACTORY.generatePublic(new X509EncodedKeySpec(packet.publicKey()));

        final byte[] sharedSecret = new byte[16];
        ThreadLocalRandom.current().nextBytes(sharedSecret);

        final Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        client.send(new ServerboundEncryptionResponse(
            cipher.doFinal(packet.verifyToken()),
            cipher.doFinal(sharedSecret)
        ));

        final SecretKey secretKey = new SecretKeySpec(sharedSecret, "AES");
        client.channel.pipeline()
            .addFirst("encrypt", new PacketEncrypter(secretKey))
            .addFirst("decrypt", new PacketDecrypter(secretKey));

        attr.set(new AwaitingIdentityRequest(
            sharedSecret,
            publicKey
        ));
    }

    // ============================================================
    // Identity Request
    // ============================================================

    record AwaitingIdentityRequest(
        byte @NotNull [] sharedSecret,
        @NotNull PublicKey publicKey
    ) implements ConnectionState {
        public AwaitingIdentityRequest {
            Objects.requireNonNull(sharedSecret);
            Objects.requireNonNull(publicKey);
        }
    }

    public static void handleIdentityRequest(
        final @NotNull Client client,
        final @NotNull ClientboundIdentityRequest packet
    ) throws Exception {
        final Attribute<ConnectionState> attr = client.channel.attr(ClientConnectionState.KEY);
        if (!(attr.getAndSet(null) instanceof final AwaitingIdentityRequest state)) {
            throw new UnexpectedPacketException(packet);
        }

        if (!Arrays.equals(packet.sharedSecret(), state.sharedSecret())) {
            throw new IllegalStateException("Shared secret did not match!");
        }

        final LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            throw new IllegalStateException("Player is missing!");
        }

        final ServerData serverData = player.connection.getServerData();
        if (serverData == null) {
            throw new IllegalStateException("Server data is missing!");
        }

        final User session = Minecraft.getInstance().getUser();
        boolean authenticated;
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            authenticated = false;
        }
        else {
            final String sha; {
                final MessageDigest md = Util.sha1();
                md.update(state.sharedSecret());
                md.update(state.publicKey().getEncoded());
                sha = HexFormat.of().formatHex(md.digest());
            }
            try {
                Minecraft.getInstance().getMinecraftSessionService().joinServer(
                    session.getProfileId(),
                    session.getAccessToken(),
                    sha
                );
                authenticated = true;
            }
            catch (final AuthenticationException e) {
                LOGGER.warn("Could not authenticate!", e);
                authenticated = false;
            }
        }

        final String namelayerName = player.getGameProfile().getName();
        final String mojangName = session.getName();
        final UUID playerUuid = session.getProfileId();

        attr.set(new ConnectionState.GreenCard(
            namelayerName,
            mojangName,
            playerUuid
        ));

        client.send(new ServerboundIdentityResponse(
            authenticated,
            namelayerName,
            mojangName,
            playerUuid,
            HostAndPort.fromString(serverData.ip).withDefaultPort(25565).toString()
        ));
    }
}
