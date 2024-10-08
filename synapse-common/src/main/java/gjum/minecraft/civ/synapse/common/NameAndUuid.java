package gjum.minecraft.civ.synapse.common;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public record NameAndUuid(
    @NotNull String name,
    @NotNull UUID uuid
) {
    public NameAndUuid {
        if (!Util.isValidMinecraftUsername(name)) {
            throw new IllegalArgumentException("Invalid username!");
        }
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null!");
        }
    }
}
