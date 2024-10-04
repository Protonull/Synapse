package gjum.minecraft.civ.synapse.common.network.packets.serverbound;

import gjum.minecraft.civ.synapse.common.network.handlers.PacketDecoder;
import gjum.minecraft.civ.synapse.common.network.handlers.PacketEncoder;
import gjum.minecraft.civ.synapse.common.network.packets.JsonPacket;
import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import java.io.DataInput;
import org.jetbrains.annotations.NotNull;

public final class ServerboundProtocol {
    private static final byte HANDSHAKE_PACKET = 0;
    private static final byte ENCRYPTION_RESPONSE_PACKET = 1;
    private static final byte JSON_PACKET = 2;

    public static final class Encoder extends PacketEncoder {
        @Override
        protected byte getPacketId(
            final @NotNull Packet packet
        ) throws Exception {
            return switch (packet) {
                case final ServerboundHandshake ignored -> HANDSHAKE_PACKET;
                case final ServerboundEncryptionResponse ignored -> ENCRYPTION_RESPONSE_PACKET;
                case final JsonPacket ignored -> JSON_PACKET;
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
                case HANDSHAKE_PACKET -> ServerboundHandshake.decode(in);
                case ENCRYPTION_RESPONSE_PACKET -> ServerboundEncryptionResponse.decode(in);
                case JSON_PACKET -> JsonPacket.decode(in);
                default -> throw new IllegalArgumentException("Unknown client packet id `" + packetId + "` 0x{}" + Integer.toHexString(packetId));
            };
        }
    }
}
