package gjum.minecraft.civ.synapse.common.observations;

import com.google.gson.annotations.Expose;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ObservationImpl implements Observation {
    @Expose
    @NotNull
    public String witness;

    @Expose
    public long time = System.currentTimeMillis();

    @Expose
    @Nullable
    public String messagePlain;

    public ObservationImpl(@NotNull String witness) {
        this.witness = witness;
    }

    @Override
    public long getTime() {
        return time;
    }

    @NotNull
    @Override
    public String getWitness() {
        return witness;
    }

    public ObservationImpl setMessagePlain(String messagePlain) {
        this.messagePlain = messagePlain;
        return this;
    }
}
