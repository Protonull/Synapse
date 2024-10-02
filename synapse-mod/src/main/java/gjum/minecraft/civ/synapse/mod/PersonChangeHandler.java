package gjum.minecraft.civ.synapse.mod;

import java.util.Collection;
import javax.annotation.Nullable;

public interface PersonChangeHandler {
	/**
	 * The person was updated (accounts, factions, standing).
	 */
	void handlePersonChange(@Nullable Person personOld, @Nullable Person personNew);

	/**
	 * Many persons were updated (config load, faction standing change).
	 */
	void handleLargeChange(Collection<Person> persons);
}
