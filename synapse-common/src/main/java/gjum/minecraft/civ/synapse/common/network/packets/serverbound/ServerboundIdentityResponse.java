package gjum.minecraft.civ.synapse.common.network.packets.serverbound;

import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import gjum.minecraft.civ.synapse.common.network.packets.PacketHelpers;
import java.io.DataInput;
import java.io.DataOutput;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Prev: {@link gjum.minecraft.civ.synapse.common.network.packets.clientbound.ClientboundIdentityRequest}
 * Next: {@link gjum.minecraft.civ.synapse.common.network.packets.clientbound.ClientboundWelcome}
 */
public record ServerboundIdentityResponse(
    boolean authenticated,
    @NotNull String namelayerUsername,
    @NotNull String mojangUsername,
    @NotNull UUID playerUuid,
    @NotNull String gameAddress
) implements Packet {
    public ServerboundIdentityResponse {
        Objects.requireNonNull(namelayerUsername); // TODO: Switch to a validation check
        Objects.requireNonNull(mojangUsername); // TODO: Switch to a validation check
        Objects.requireNonNull(playerUuid);
        Objects.requireNonNull(gameAddress);
    }

    @Override
    public void encode(
        final @NotNull DataOutput out
    ) throws Exception {
        out.writeBoolean(authenticated());
        out.writeUTF(namelayerUsername());
        out.writeUTF(mojangUsername());
        PacketHelpers.writeUuid(out, playerUuid());
        out.writeUTF(gameAddress());
    }

    public static @NotNull ServerboundIdentityResponse decode(
        final @NotNull DataInput in
    ) throws Exception {
        return new ServerboundIdentityResponse(
            in.readBoolean(),
            in.readUTF(),
            in.readUTF(),
            PacketHelpers.readUuid(in),
            in.readUTF()
        );
    }
}
