package gjum.minecraft.civ.synapse.common.observations.accountpos;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.Pos;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Info about self, and about friendlies received via relay.
 */
public class PlayerStateExtra extends PlayerState implements AccountPosObservation {
	@Expose
	public static final String msgType = "PlayerStateExtra";

	@Expose
	@Nullable
	public String heading; // TODO more finer heading resolution
	@Expose
	@Nullable
	public Float health = -1f;
	@Expose
	@Nullable
	public Integer hpotCount = -1;
	@Expose
	@Nullable
	public Integer minArmorDura = -1;
	@Expose
	@Nullable
	public Long combatTagEnd = -1L;

	public PlayerStateExtra(
			@NotNull String witness,
			@NotNull String account,
			@NotNull Pos pos,
			@NotNull String world
	) {
		super(witness, account, pos, world);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		PlayerStateExtra other = (PlayerStateExtra) o;
		return Objects.equals(heading, other.heading) &&
				Objects.equals(health, other.health) &&
				Objects.equals(hpotCount, other.hpotCount) &&
				Objects.equals(minArmorDura, other.minArmorDura) &&
				Objects.equals(combatTagEnd, other.combatTagEnd);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), heading, health, hpotCount, minArmorDura, combatTagEnd);
	}

	@Override
	public String getMsgType() {
		return msgType;
	}

	/**
	 * Returns a copy of this with null fields filled with info from the other state, where present.
	 */
	@NotNull
	public PlayerStateExtra fillMissing(@NotNull PlayerStateExtra other) {
		final PlayerStateExtra combined = new PlayerStateExtra(witness, account, pos, world);
		combined.heading = heading != null ? heading : other.heading;
		combined.combatTagEnd = combatTagEnd != null ? combatTagEnd : other.combatTagEnd;
		combined.health = health != null ? health : other.health;
		combined.hpotCount = hpotCount != null ? hpotCount : other.hpotCount;
		combined.minArmorDura = minArmorDura != null ? minArmorDura : other.minArmorDura;
		return combined;
	}
}
