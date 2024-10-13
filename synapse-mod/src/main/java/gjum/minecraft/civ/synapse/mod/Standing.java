package gjum.minecraft.civ.synapse.mod;

import org.jetbrains.annotations.NotNull;

/**
 * sorted by confidence
 */
public enum Standing {
    HOSTILE,
    FRIENDLY,
    NEUTRAL,
    UNSET;

    public boolean moreConfidentThan(
        final Standing other
    ) {
        return other == null || compareTo(other) < 0;
    }

    public interface Holder {
        @NotNull Standing synapse$getStanding();

        void synapse$setStanding(
            @NotNull Standing standing
        );
    }
}
