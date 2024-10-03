package gjum.minecraft.civ.synapse.mod.integrations.combatradar;

import com.aleksey.combatradar.Radar;
import com.aleksey.combatradar.config.PlayerType;
import com.aleksey.combatradar.config.RadarConfig;
import gjum.minecraft.civ.synapse.mod.LiteModSynapse;
import gjum.minecraft.civ.synapse.mod.Person;
import gjum.minecraft.civ.synapse.mod.PersonChangeHandler;
import gjum.minecraft.civ.synapse.mod.Standing;
import gjum.minecraft.civ.synapse.mod.config.ServerConfig;
import gjum.minecraft.civ.synapse.mod.integrations.IntegrationHelpers;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public final class CombatRadarHelpers implements PersonChangeHandler {
	@Override
	public void handlePersonChange(
		final Person oldPerson,
		final Person newPerson
	) {
		if (!IntegrationHelpers.isCombatRadarPresent()) {
			return;
		}
		final ServerConfig serverConfig = LiteModSynapse.instance.serverConfig;
		if (serverConfig == null) {
			return;
		}
		final PlayerType newType = getPlayerTypeFromStanding(serverConfig.getStanding(newPerson));
		final RadarConfig radarConfig = Radar.getConfig();
//		// mark removed accounts as NEUTRAL
//		if (oldPerson != null) {
//			for (String account : oldPerson.getAccounts()) {
//				if (newPerson == null || newPerson.hasAccount(account) == null) {
//					// account was removed and is now not associated anymore
//					radarConfig.setPlayerType(account, PlayerType.Neutral);
//				}
//			}
//		}
		if (newPerson != null) {
			for (final String account : newPerson.getAccounts()) {
				radarConfig.setPlayerType(account, newType);
			}
		}
	}

	@Override
	public void handleLargeChange(
		final @NotNull Collection<@NotNull Person> persons
	) {
		if (!IntegrationHelpers.isCombatRadarPresent()) {
			return;
		}
		final ServerConfig serverConfig = LiteModSynapse.instance.serverConfig;
		if (serverConfig == null) {
			return;
		}
		final RadarConfig radarConfig = Radar.getConfig();
		for (final Person person : persons) {
			final PlayerType playerType = getPlayerTypeFromStanding(serverConfig.getStanding(person));
			// leave alone the existing accounts that don't exist in PersonsRegistry
			for (final String account : person.getAccounts()) {
				radarConfig.setPlayerType(account, playerType);
			}
		}
	}

	private static @NotNull PlayerType getPlayerTypeFromStanding(
		final @NotNull Standing standing
	) {
        return switch (standing) {
            case FRIENDLY -> PlayerType.Ally;
            case HOSTILE -> PlayerType.Enemy;
            default -> PlayerType.Neutral;
        };
	}
}
