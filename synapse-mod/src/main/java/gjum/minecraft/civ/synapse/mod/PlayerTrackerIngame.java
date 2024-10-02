package gjum.minecraft.civ.synapse.mod;

import static gjum.minecraft.civ.synapse.mod.McUtil.getEntityPosition;
import static gjum.minecraft.civ.synapse.mod.McUtil.getMc;
import static gjum.minecraft.civ.synapse.mod.McUtil.getSelfAccount;

import gjum.minecraft.civ.synapse.common.observations.PlayerTracker;
import gjum.minecraft.civ.synapse.common.observations.accountpos.AccountPosObservation;
import gjum.minecraft.civ.synapse.common.observations.accountpos.PlayerState;
import net.minecraft.entity.player.EntityPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerTrackerIngame extends PlayerTracker {
	public PlayerTrackerIngame(@Nullable String gameAddress) {
		super(gameAddress);
	}

	@Override
	@Nullable
	public AccountPosObservation getMostRecentPosObservationForAccount(@NotNull String account) {
		if (getMc().world != null) {
			final EntityPlayer player = getMc().world.getPlayerEntityByName(account);
			if (player != null) {
				return new PlayerState(getSelfAccount(),
						account, getEntityPosition(player), LiteModSynapse.instance.worldName);
			}
		}
		return super.getMostRecentPosObservationForAccount(account);
	}

	// TODO override getLastObservationBeforeWithSignificantMove and use radar pos
}
