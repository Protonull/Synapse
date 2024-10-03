package gjum.minecraft.civ.synapse.common.observations;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Something a player can do.
 */
public enum Action {
    ENTERED(""), LOGIN("+"), LOGOUT("-"), CTLOG("c"),
    NEWSPAWN("n"), APPEARED("+"), DISAPPEARED("-");

    public final String shortName;

    Action(String shortName) {
        this.shortName = shortName;
    }

    @Nullable
    public static Action fromString(@NotNull String actionStr) {
        switch (actionStr) {
            case "entered":
                return ENTERED;
            case "logged in":
                return LOGIN;
            case "logged out":
                return LOGOUT;
            case "new":
                return NEWSPAWN;
            default:
                System.err.println("Unexpected action " + actionStr);
                return null;
        }
    }
}
