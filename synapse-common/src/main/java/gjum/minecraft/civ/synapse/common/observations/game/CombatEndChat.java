package gjum.minecraft.civ.synapse.common.observations.game;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.common.observations.ObservationImpl;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class CombatEndChat extends ObservationImpl {
	@Expose
	public static final String msgType = "CombatEndChat";

	public CombatEndChat(
			@NotNull String witness
	) {
		super(witness);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof CombatEndChat)) return false;
		if (!super.equals(o)) return false;
		return true;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode());
	}

	@Override
	public String getMsgType() {
		return msgType;
	}
}
