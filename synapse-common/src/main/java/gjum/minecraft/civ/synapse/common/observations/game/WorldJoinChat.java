package gjum.minecraft.civ.synapse.common.observations.game;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.observations.ObservationImpl;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class WorldJoinChat extends ObservationImpl {
    @Expose
    public static final String msgType = "WorldJoinChat";

    @Expose
    @NotNull
    public final String world;

    public WorldJoinChat(
            @NotNull String witness,
            @NotNull String world
    ) {
        super(witness);
        this.world = world;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorldJoinChat)) return false;
        if (!super.equals(o)) return false;
        WorldJoinChat other = (WorldJoinChat) o;
        return Objects.equals(world, other.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), world);
    }

    @Override
    public String getMsgType() {
        return msgType;
    }
}
