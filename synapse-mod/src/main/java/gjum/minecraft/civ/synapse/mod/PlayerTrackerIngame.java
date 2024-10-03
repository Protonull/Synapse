package gjum.minecraft.civ.synapse.mod;

import gjum.minecraft.civ.synapse.common.observations.PlayerTracker;
import gjum.minecraft.civ.synapse.common.observations.accountpos.AccountPosObservation;
import gjum.minecraft.civ.synapse.common.observations.accountpos.PlayerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlayerTrackerIngame extends PlayerTracker {
	public PlayerTrackerIngame(
		final String gameAddress
	) {
		super(gameAddress);
	}

	@Override
	public @Nullable AccountPosObservation getMostRecentPosObservationForAccount(
		final @NotNull String account
	) {
		final ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return super.getMostRecentPosObservationForAccount(account);
		}
		final AbstractClientPlayer found = McUtil.findFirstPlayerByName(level, account);
		if (found == null) {
			return super.getMostRecentPosObservationForAccount(account);
		}
		return new PlayerState(
			McUtil.getSelfAccount(),
			account,
			McUtil.getEntityPosition(found),
			LiteModSynapse.instance.worldName
		);
	}

	// TODO override getLastObservationBeforeWithSignificantMove and use radar pos
}
