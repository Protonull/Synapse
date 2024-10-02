package gjum.minecraft.civ.synapse.common.packet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import gjum.minecraft.civ.synapse.common.observations.accountpos.PearlTransport;
import gjum.minecraft.civ.synapse.common.observations.accountpos.PlayerState;
import gjum.minecraft.civ.synapse.common.observations.accountpos.PlayerStateExtra;
import gjum.minecraft.civ.synapse.common.observations.accountpos.RadarChange;
import gjum.minecraft.civ.synapse.common.observations.accountpos.SnitchHit;
import gjum.minecraft.civ.synapse.common.observations.game.BastionChat;
import gjum.minecraft.civ.synapse.common.observations.game.BrandNew;
import gjum.minecraft.civ.synapse.common.observations.game.CombatEndChat;
import gjum.minecraft.civ.synapse.common.observations.game.CombatTagChat;
import gjum.minecraft.civ.synapse.common.observations.game.GroupChat;
import gjum.minecraft.civ.synapse.common.observations.game.PearlLocation;
import gjum.minecraft.civ.synapse.common.observations.game.PearledChat;
import gjum.minecraft.civ.synapse.common.observations.game.Skynet;
import gjum.minecraft.civ.synapse.common.observations.game.WorldJoinChat;
import gjum.minecraft.civ.synapse.common.observations.instruction.FocusAnnouncement;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonPacket extends Packet {
	private static final JsonDeserializer<Object> deserializer = new JsonDeserializer<Object>() {
		@Override
		public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			final String msgType = json.getAsJsonObject().get("msgType").getAsString();
			final Class<?> typeClass = getTypeClassForMsgType(msgType);
			if (typeClass == null) return null;
			return serializerGson.fromJson(json, typeClass);
		}
	};

	private static final Gson serializerGson = new GsonBuilder()
			.excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT) // only excluding transient means that we include static, which is what we actually want
			.excludeFieldsWithoutExposeAnnotation()
			.registerTypeAdapter(Object.class, deserializer)
			.create();

	private static final Gson deserializerGson = new GsonBuilder()
			.excludeFieldsWithoutExposeAnnotation()
			// ignore static fields when deserializing; i.e., use default settings
			.create();

	@Nullable
	String jsonText;
	@Nullable
	private Object payload;

	public JsonPacket(@Nullable Object payload) {
		this.payload = payload;
	}

	public static Packet read(ByteBuf buf) {
		final JsonPacket packet = new JsonPacket(null); // only parse when someone calls getPayload()
		packet.jsonText = readOptionalString(buf);
		return packet;
	}

	@Override
	public void write(ByteBuf buf) {
		writeOptionalString(buf, getJsonText());
	}

	@NotNull
	public String getJsonText() {
		if (jsonText == null) jsonText = serializerGson.toJson(payload);
		return jsonText;
	}

	@Nullable
	public Object getPayload() {
		if (payload == null) {
			final JsonElement json = deserializerGson.fromJson(jsonText, JsonElement.class);
			final String msgType = json.getAsJsonObject().get("msgType").getAsString();
			payload = deserializerGson.fromJson(json, getTypeClassForMsgType(msgType));
		}
		return payload;
	}

	@Override
	public String toString() {
		return "JsonPacket" + getJsonText();
	}

	@Nullable
	static Class<?> getTypeClassForMsgType(String msgType) {
		switch (msgType) {
			case BastionChat.msgType:
				return BastionChat.class;
			case BrandNew.msgType:
				return BrandNew.class;
			case CombatEndChat.msgType:
				return CombatEndChat.class;
			case CombatTagChat.msgType:
				return CombatTagChat.class;
			case FocusAnnouncement.msgType:
				return FocusAnnouncement.class;
			case GroupChat.msgType:
				return GroupChat.class;
			case PearlLocation.msgType:
				return PearlLocation.class;
			case PearlTransport.msgType:
				return PearlTransport.class;
			case PearledChat.msgType:
				return PearledChat.class;
			case PlayerState.msgType:
				return PlayerState.class;
			case PlayerStateExtra.msgType:
				return PlayerStateExtra.class;
			case RadarChange.msgType:
				return RadarChange.class;
			case Skynet.msgType:
				return Skynet.class;
			case SnitchHit.msgType:
				return SnitchHit.class;
			case WorldJoinChat.msgType:
				return WorldJoinChat.class;
			default:
				return null;
		}
	}
}
