package gjum.minecraft.civ.synapse.common.observations.game;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.Pos;
import gjum.minecraft.civ.synapse.common.observations.ObservationImpl;
import gjum.minecraft.civ.synapse.common.observations.PosObservation;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class BastionChat extends ObservationImpl implements PosObservation {
    @Expose
    public static final String msgType = "BastionChat";

    @Expose
    @NotNull
    public final Pos pos;

    @Expose
    @NotNull
    public final String world;

    public enum State {NONE, FRIENDLY, HOSTILE}

    @Expose
    @NotNull
    public final State bastionState;

    public enum Source {INFO, BLOCK, BOAT, PEARL}

    @Expose
    @NotNull
    public final Source bastionSource;

    public BastionChat(
            @NotNull String witness,
            @NotNull Pos pos,
            @NotNull String world,
            @NotNull State bastionState,
            @NotNull Source bastionSource
    ) {
        super(witness);
        this.pos = pos;
        this.world = world;
        this.bastionState = bastionState;
        this.bastionSource = bastionSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BastionChat)) return false;
        if (!super.equals(o)) return false;
        final BastionChat other = (BastionChat) o;
        return Objects.equals(pos, other.pos)
                && Objects.equals(world, other.world)
                && Objects.equals(bastionState, other.bastionState)
                && Objects.equals(bastionSource, other.bastionSource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), pos, world, bastionState, bastionSource);
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
