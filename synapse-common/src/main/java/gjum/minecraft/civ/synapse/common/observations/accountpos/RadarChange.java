package gjum.minecraft.civ.synapse.common.observations.accountpos;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.Pos;
import gjum.minecraft.civ.synapse.common.observations.Action;
import gjum.minecraft.civ.synapse.common.observations.ObservationImpl;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class RadarChange extends ObservationImpl implements AccountPosObservation {
	@Expose
	public static final String msgType = "RadarChange";

	@Expose
	@NotNull
	public final String account;
	@Expose
	@NotNull
	public final Pos pos;
	@Expose
	@NotNull
	public final String world;
	@Expose
	@NotNull
	public final Action action;

	public RadarChange(
			@NotNull String witness,
			@NotNull String account,
			@NotNull Pos pos,
			@NotNull String world,
			@NotNull Action action
	) {
		super(witness);
		this.account = account;
		this.pos = pos;
		this.world = world;
		this.action = action;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		RadarChange other = (RadarChange) o;
		return account.equals(other.account) &&
				pos.equals(other.pos) &&
				world.equals(other.world) &&
				action.equals(other.action);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), account, pos, world, action);
	}

	@Override
	public String getMsgType() {
		return msgType;
	}

	@NotNull
	@Override
	public String getAccount() {
		return account;
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
