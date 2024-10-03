package gjum.minecraft.civ.synapse.common.observations.accountpos;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.Pos;
import gjum.minecraft.civ.synapse.common.observations.game.PearlLocation;
import org.jetbrains.annotations.NotNull;

public class PearlTransport extends PearlLocation implements AccountPosObservation {
    @Expose
    public static final String msgType = "PearlTransport";

    public PearlTransport(
            @NotNull String witness,
            @NotNull Pos pos,
            @NotNull String world,
            @NotNull String prisoner,
            @NotNull String holder
    ) {
        super(witness, pos, world, prisoner, holder);
        if (!isPlayerHolder()) {
            throw new IllegalArgumentException(
                    "Pearl holder '" + holder + "' is not a player");
        }
    }

    @NotNull
    @Override
    public String getAccount() {
        return holder;
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
