package gjum.minecraft.civ.synapse.common.network.packets.serverbound;

import gjum.minecraft.civ.synapse.common.network.packets.Packet;
import java.io.DataInput;
import java.io.DataOutput;
import java.util.Objects;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNull;

/**
 * Should send after {@link gjum.minecraft.civ.synapse.common.network.packets.clientbound.ClientboundWelcome} or when
 * the user has changed a relevant config value. Without sending this packet, the server should presume the most
 * restrictive config settings.
 */
public record ServerboundSetConfig(
    @NotNull CompoundBinaryTag config
) implements Packet {
    public ServerboundSetConfig {
        Objects.requireNonNull(config);
    }

    @Override
    public void encode(
        final @NotNull DataOutput out
    ) throws Exception {
        BinaryTagTypes.COMPOUND.write(config(), out);
    }

    public static @NotNull ServerboundSetConfig decode(
        final @NotNull DataInput in
    ) throws Exception {
        return new ServerboundSetConfig(
            BinaryTagTypes.COMPOUND.read(in)
        );
    }
}
