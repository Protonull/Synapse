package gjum.minecraft.civ.synapse.common.observations;

import org.jetbrains.annotations.Nullable;

public interface GroupObservation extends Observation {
    @Nullable
    String getGroup();
}
