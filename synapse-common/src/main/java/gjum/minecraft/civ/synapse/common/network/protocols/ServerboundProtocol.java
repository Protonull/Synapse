package gjum.minecraft.civ.synapse.common.network.protocols;

import gjum.minecraft.civ.synapse.common.network.handlers.PacketDecoder;
import gjum.minecraft.civ.synapse.common.network.handlers.PacketEncoder;
import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundBeginHandshake;
import gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundEncryptionResponse;
import gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundIdentityResponse;
import gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundReportPosition;
import gjum.minecraft.civ.synapse.common.network.packets.serverbound.ServerboundSetConfig;
import java.io.DataInput;
import org.jetbrains.annotations.NotNull;

public final class ServerboundProtocol {
    public static final class Encoder extends PacketEncoder {
        @Override
        protected byte getPacketId(
            final @NotNull Packet packet
        ) throws Exception {
            return switch (packet) {
                // Handshake
                case final ServerboundBeginHandshake ignored -> Packet.SERVERBOUND_BEGIN_HANDSHAKE;
                case final ServerboundEncryptionResponse ignored -> Packet.SERVERBOUND_ENCRYPTION_RESPONSE;
                case final ServerboundIdentityResponse ignored -> Packet.SERVERBOUND_IDENTITY_RESPONSE;
                // Config
                case final ServerboundSetConfig ignored -> Packet.SERVERBOUND_SET_CONFIG;
                // Points of Interest
                case final ServerboundReportPosition ignored -> Packet.SERVERBOUND_REPORT_POSITION;
                default -> throw new IllegalArgumentException("Unknown client packet class '" + packet.getClass().getName() + "': " + packet);
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
                // Handshake
                case Packet.SERVERBOUND_BEGIN_HANDSHAKE -> ServerboundBeginHandshake.decode(in);
                case Packet.SERVERBOUND_ENCRYPTION_RESPONSE -> ServerboundEncryptionResponse.decode(in);
                case Packet.SERVERBOUND_IDENTITY_RESPONSE -> ServerboundIdentityResponse.decode(in);
                // Config
                case Packet.SERVERBOUND_SET_CONFIG -> ServerboundSetConfig.decode(in);
                // Points of Interest
                case Packet.SERVERBOUND_REPORT_POSITION -> ServerboundReportPosition.decode(in);
                default -> throw new IllegalArgumentException("Unknown client packet id `" + packetId + "` 0x" + Integer.toHexString(packetId));
            };
        }
    }
}
