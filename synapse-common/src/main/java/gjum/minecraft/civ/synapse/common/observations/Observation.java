package gjum.minecraft.civ.synapse.common.observations;

import org.jetbrains.annotations.NotNull;

public interface Observation {
    long getTime();

    @NotNull
    String getWitness();

    String getMsgType();
}
