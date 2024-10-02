package gjum.minecraft.civ.synapse.common.observations.game;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.observations.AccountObservation;
import gjum.minecraft.civ.synapse.common.observations.Action;
import gjum.minecraft.civ.synapse.common.observations.ObservationImpl;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Skynet extends ObservationImpl implements AccountObservation {
	@Expose
	public static final String msgType = "Skynet";

	@Expose
	@NotNull
	public final UUID uuid;

	@Expose
	@NotNull
	public final String account;

	@Expose
	@NotNull
	public final Action action;

	@Expose
	@Nullable
	public final Integer gamemode;

	public Skynet(
			@NotNull String witness,
			@NotNull UUID uuid,
			@NotNull String account,
			@NotNull Action action,
			@Nullable Integer gamemode
	) {
		super(witness);
		this.uuid = uuid;
		this.account = account;
		this.action = action;
		this.gamemode = gamemode;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Skynet)) return false;
		if (!super.equals(o)) return false;
		Skynet other = (Skynet) o;
		return Objects.equals(uuid, other.uuid)
				&& action == other.action
				&& Objects.equals(account, other.account);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), uuid, action, account);
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
}
