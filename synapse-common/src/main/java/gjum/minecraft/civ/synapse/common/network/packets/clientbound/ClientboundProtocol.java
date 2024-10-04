package gjum.minecraft.civ.synapse.common.network.packets.clientbound;

import gjum.minecraft.civ.synapse.common.network.handlers.PacketDecoder;
import gjum.minecraft.civ.synapse.common.network.handlers.PacketEncoder;
import gjum.minecraft.civ.synapse.common.network.packets.JsonPacket;
import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import java.io.DataInput;
import org.jetbrains.annotations.NotNull;

public final class ClientboundProtocol {
    private static final byte ENCRYPTION_REQUEST_PACKET = 0;
    private static final byte JSON_PACKET = 1;

    public static final class Encoder extends PacketEncoder {
        @Override
        protected byte getPacketId(
            final @NotNull Packet packet
        ) throws Exception {
            return switch (packet) {
                case final ClientboundEncryptionRequest ignored -> ENCRYPTION_REQUEST_PACKET;
                case final JsonPacket ignored -> JSON_PACKET;
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
                case ENCRYPTION_REQUEST_PACKET -> ClientboundEncryptionRequest.decode(in);
                case JSON_PACKET -> JsonPacket.decode(in);
                default -> throw new IllegalArgumentException("Unknown server packet id `" + packetId + "` 0x{}" + Integer.toHexString(packetId));
            };
        }
    }
}
