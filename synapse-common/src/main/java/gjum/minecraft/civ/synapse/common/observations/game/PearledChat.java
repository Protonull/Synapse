package gjum.minecraft.civ.synapse.common.observations.game;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.observations.AccountObservation;
import gjum.minecraft.civ.synapse.common.observations.ObservationImpl;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PearledChat extends ObservationImpl implements AccountObservation {
    @Expose
    public static final String msgType = "PearledChat";

    @Expose
    @NotNull
    public final String holder;
    @Expose
    @Nullable
    public final String pearlType;

    public PearledChat(
            @NotNull String witness,
            @NotNull String holder,
            @Nullable String pearlType
    ) {
        super(witness);
        this.holder = holder;
        this.pearlType = pearlType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PearledChat)) return false;
        if (!super.equals(o)) return false;
        PearledChat other = (PearledChat) o;
        return Objects.equals(this.holder, other.holder)
                && Objects.equals(this.pearlType, other.pearlType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), holder, pearlType);
    }

    @Override
    public String getMsgType() {
        return msgType;
    }

    @NotNull
    @Override
    public String getAccount() {
        return holder;
    }
}
