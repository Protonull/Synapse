package gjum.minecraft.civ.synapse.common.observations.game;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.Pos;
import gjum.minecraft.civ.synapse.common.observations.ObservationImpl;
import gjum.minecraft.civ.synapse.common.observations.PosObservation;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class PearlLocation extends ObservationImpl implements PosObservation {
    @Expose
    public static final String msgType = "PearlLocation";

    @Expose
    @NotNull
    public final Pos pos;
    @Expose
    @NotNull
    public final String world;
    @Expose
    @NotNull
    public final String prisoner;
    @Expose
    @NotNull
    public final String holder;

    public PearlLocation(
            @NotNull String witness,
            @NotNull Pos pos,
            @NotNull String world,
            @NotNull String prisoner,
            @NotNull String holder
    ) {
        super(witness);
        this.pos = pos;
        this.world = world;
        this.prisoner = prisoner;
        this.holder = holder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PearlLocation other = (PearlLocation) o;
        return prisoner.equals(other.prisoner) &&
                holder.equals(other.holder) &&
                pos.equals(other.pos) &&
                world.equals(other.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), prisoner, holder, pos, world);
    }

    public static boolean isPlayerHolder(@NotNull String holder) {
        return !holder.equals("nobody") && !holder.contains(" ");
    }

    public boolean isPlayerHolder() {
        return isPlayerHolder(holder);
    }

    @Override
    public String getMsgType() {
        return msgType;
    }

    @NotNull
    @Override
    public Pos getPos() {
        return pos;
    }

    @NotNull
    @Override
    public String getWorld() {
        return world;
    }
}
