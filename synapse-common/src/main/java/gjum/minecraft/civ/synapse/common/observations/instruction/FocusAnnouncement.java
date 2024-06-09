package gjum.minecraft.civ.synapse.common.observations.instruction;

import static gjum.minecraft.civ.synapse.common.Util.sortedUniqListIgnoreCase;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.observations.ObservationImpl;
import java.util.Collection;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class FocusAnnouncement extends ObservationImpl {
	@Expose
	public static final String msgType = "FocusAnnouncement";

	@Expose
	@NotNull
	public final Collection<String> accounts;

	public FocusAnnouncement(
			@NotNull String witness,
			@NotNull Collection<String> accounts
	) {
		super(witness);
		accounts = sortedUniqListIgnoreCase(accounts);
		this.accounts = accounts;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof FocusAnnouncement)) return false;
		if (!super.equals(o)) return false;
		FocusAnnouncement other = (FocusAnnouncement) o;
		return Objects.equals(accounts, other.accounts);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), accounts);
	}

	@Override
	public String getMsgType() {
		return msgType;
	}
}
