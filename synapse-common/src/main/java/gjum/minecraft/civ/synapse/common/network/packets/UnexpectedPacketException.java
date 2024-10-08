package gjum.minecraft.civ.synapse.common.network.packets;

import org.jetbrains.annotations.NotNull;

public final class UnexpectedPacketException extends RuntimeException {
    public UnexpectedPacketException(
        final @NotNull Packet receivedPacket
    ) {
        this("Unexpected packet [" + receivedPacket.getClass().getName() + "]!");
    }

    public UnexpectedPacketException(
        final @NotNull String message
    ) {
        super(message);
    }
}
