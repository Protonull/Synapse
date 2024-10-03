package gjum.minecraft.civ.synapse.mod;

/**
 * sorted by confidence
 */
public enum Standing {
    FOCUS,
    HOSTILE,
    FRIENDLY,
    NEUTRAL,
    UNSET;

    public boolean moreConfidentThan(
        final Standing other
    ) {
        return other == null || compareTo(other) < 0;
    }
}
