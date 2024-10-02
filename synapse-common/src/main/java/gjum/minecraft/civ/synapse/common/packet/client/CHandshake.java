package gjum.minecraft.civ.synapse.common.packet.client;

import gjum.minecraft.civ.synapse.common.packet.Packet;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CHandshake extends Packet {
	@Nullable
	public final String synapseVersion;
	@NotNull
	public final String username;
	@Nullable
	public final String gameAddress;

	public CHandshake(@Nullable String synapseVersion, @Nullable String username, @Nullable String gameAddress) {
		this.synapseVersion = synapseVersion;
		this.username = username == null ? "" : username;
		this.gameAddress = gameAddress;
	}

	public static Packet read(ByteBuf buf) {
		return new CHandshake(
			readOptionalString(buf),
			readOptionalString(buf),
			readOptionalString(buf));
	}

	@Override
	public void write(ByteBuf out) {
		writeOptionalString(out, synapseVersion);
		writeOptionalString(out, username);
		writeOptionalString(out, gameAddress);
	}

	@Override
	public String toString() {
		return "CHandshake{" +
			"version='" + synapseVersion + '\'' +
			" username='" + username + '\'' +
			" gameAddress='" + gameAddress + '\'' +
			'}';
	}
}
