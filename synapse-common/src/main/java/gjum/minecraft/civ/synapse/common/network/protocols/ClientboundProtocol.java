package gjum.minecraft.civ.synapse.common.network.protocols;

import gjum.minecraft.civ.synapse.common.network.handlers.PacketDecoder;
import gjum.minecraft.civ.synapse.common.network.handlers.PacketEncoder;
import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import gjum.minecraft.civ.synapse.common.network.packets.clientbound.ClientboundEncryptionRequest;
import gjum.minecraft.civ.synapse.common.network.packets.clientbound.ClientboundIdentityRequest;
import gjum.minecraft.civ.synapse.common.network.packets.clientbound.ClientboundKick;
import gjum.minecraft.civ.synapse.common.network.packets.clientbound.ClientboundWelcome;
import java.io.DataInput;
import org.jetbrains.annotations.NotNull;

public final class ClientboundProtocol {
    public static final class Encoder extends PacketEncoder {
        @Override
        protected byte getPacketId(
            final @NotNull Packet packet
        ) throws Exception {
            return switch (packet) {
                case final ClientboundKick ignored -> Packet.CLIENTBOUND_KICK;
                // Handshake
                case final ClientboundEncryptionRequest ignored -> Packet.CLIENTBOUND_ENCRYPTION_REQUEST;
                case final ClientboundIdentityRequest ignored -> Packet.CLIENTBOUND_IDENTITY_REQUEST;
                case final ClientboundWelcome ignored -> Packet.CLIENTBOUND_WELCOME;
                default -> throw new IllegalArgumentException("Unknown server packet class '" + packet.getClass().getName() + "': " + packet);
            };
        }
    }

    public static final class Decoder extends PacketDecoder {
        @Override
        protected @NotNull Packet decodePacket(
            final @NotNull DataInput in,
            final byte packetId
        ) throws Exception {
            return switch (packetId) {
                case Packet.CLIENTBOUND_KICK -> ClientboundKick.decode(in);
                // Handshake
                case Packet.CLIENTBOUND_ENCRYPTION_REQUEST -> ClientboundEncryptionRequest.decode(in);
                case Packet.CLIENTBOUND_IDENTITY_REQUEST -> ClientboundIdentityRequest.decode(in);
                case Packet.CLIENTBOUND_WELCOME -> ClientboundWelcome.decode(in);
                default -> throw new IllegalArgumentException("Unknown server packet id `" + packetId + "` 0x" + Integer.toHexString(packetId));
            };
        }
    }
}
