package gjum.minecraft.civ.synapse.common.observations;

import gjum.minecraft.civ.synapse.common.Pos;
import org.jetbrains.annotations.NotNull;

public interface PosObservation extends Observation {
    @NotNull
    Pos getPos();

    @NotNull
    String getWorld();
}
