package gjum.minecraft.civ.synapse.mod;

import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public interface PersonChangeHandler {
	/**
	 * The person was updated (accounts, factions, standing).
	 */
	void handlePersonChange(
		Person personOld,
		Person personNew
	);

	/**
	 * Many persons were updated (config load, faction standing change).
	 */
	void handleLargeChange(
		@NotNull Collection<@NotNull Person> persons
	);
}
