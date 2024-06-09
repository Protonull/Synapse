package gjum.minecraft.civ.synapse.common.packet.server;

import gjum.minecraft.civ.synapse.common.packet.Packet;
import io.netty.buffer.ByteBuf;
import java.security.PublicKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SEncryptionRequest extends Packet {
	@NotNull
	public final PublicKey key;
	@NotNull
	public final byte[] verifyToken;
	@Nullable
	public final String message;

	public SEncryptionRequest(@NotNull PublicKey key, @NotNull byte[] verifyToken, @Nullable String message) {
		this.key = key;
		this.verifyToken = verifyToken;
		this.message = message;
	}

	public static Packet read(ByteBuf buf) {
		return new SEncryptionRequest(
				readKey(buf),
				readByteArray(buf),
				readOptionalString(buf));
	}

	@Override
	public void write(ByteBuf buf) {
		writeByteArray(buf, key.getEncoded());
		writeByteArray(buf, verifyToken);
		writeOptionalString(buf, message);
	}

	@Override
	public String toString() {
		return "SEncryptionRequest{message='" + message + "'}";
	}
}
