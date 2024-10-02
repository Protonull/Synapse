package gjum.minecraft.civ.synapse.common.observations.game;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.observations.AccountObservation;
import gjum.minecraft.civ.synapse.common.observations.GroupObservation;
import gjum.minecraft.civ.synapse.common.observations.ObservationImpl;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GroupChat extends ObservationImpl implements AccountObservation, GroupObservation {
	@Expose
	public static final String msgType = "GroupChat";

	@Expose
	@Nullable
	public final String group;
	@Expose
	@NotNull
	public final String account;
	@Expose
	@NotNull
	public final String message;

	public GroupChat(
			@NotNull String witness,
			@Nullable String group,
			@NotNull String account,
			@NotNull String message
	) {
		super(witness);
		this.group = group;
		this.account = account;
		this.message = message;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		GroupChat other = (GroupChat) o;
		return Objects.equals(group, other.group) &&
				Objects.equals(account, other.account) &&
				Objects.equals(message, other.message);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), group, account, message);
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

	@Nullable
	@Override
	public String getGroup() {
		return group;
	}
}
