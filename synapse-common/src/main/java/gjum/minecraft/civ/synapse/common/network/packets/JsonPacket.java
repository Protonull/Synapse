package gjum.minecraft.civ.synapse.common.network.packets;

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
import java.io.DataInput;
import java.io.DataOutput;
import java.lang.reflect.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonPacket implements Packet {
    private static final Gson serializerGson = new GsonBuilder()
        .excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT) // only excluding transient means that we include static, which is what we actually want
        .excludeFieldsWithoutExposeAnnotation()
        .registerTypeAdapter(Object.class, new JsonDeserializer<>() {
            @Override
            public Object deserialize(
                final JsonElement json,
                final Type typeOfT,
                final JsonDeserializationContext context
            ) throws JsonParseException {
                final String msgType = json.getAsJsonObject().get("msgType").getAsString();
                final Class<?> typeClass = getTypeClassForMsgType(msgType);
                if (typeClass == null) return null;
                return serializerGson.fromJson(json, typeClass);
            }
        })
        .create();

    private static final Gson deserializerGson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        // ignore static fields when deserializing; i.e., use default settings
        .create();

    private String jsonText;
    private Object payload;

    public JsonPacket(
        final Object payload
    ) {
        this.payload = payload;
    }

    @Override
    public void encode(
        final @NotNull DataOutput out
    ) throws Exception {
        out.writeUTF(getJsonText());
    }

    public static @NotNull JsonPacket decode(
        final @NotNull DataInput in
    ) throws Exception {
        final var packet = new JsonPacket(null);
        packet.jsonText = in.readUTF();
        return packet;
    }

    @NotNull
    public String getJsonText() {
        if (this.jsonText == null) {
            this.jsonText = serializerGson.toJson(this.payload);
        }
        return this.jsonText;
    }

    public @Nullable Object getPayload() {
        if (this.payload == null && this.jsonText != null) {
            final JsonElement json = deserializerGson.fromJson(this.jsonText, JsonElement.class);
            final String msgType = json.getAsJsonObject().get("msgType").getAsString();
            this.payload = deserializerGson.fromJson(json, getTypeClassForMsgType(msgType));
        }
        return this.payload;
    }

    @Override
    public @NotNull String toString() {
        return "JsonPacket" + getJsonText();
    }

    static @Nullable Class<?> getTypeClassForMsgType(
        final String msgType
    ) {
        return switch (msgType) {
            case BastionChat.msgType -> BastionChat.class;
            case BrandNew.msgType -> BrandNew.class;
            case CombatEndChat.msgType -> CombatEndChat.class;
            case CombatTagChat.msgType -> CombatTagChat.class;
            case FocusAnnouncement.msgType -> FocusAnnouncement.class;
            case GroupChat.msgType -> GroupChat.class;
            case PearlLocation.msgType -> PearlLocation.class;
            case PearlTransport.msgType -> PearlTransport.class;
            case PearledChat.msgType -> PearledChat.class;
            case PlayerState.msgType -> PlayerState.class;
            case PlayerStateExtra.msgType -> PlayerStateExtra.class;
            case RadarChange.msgType -> RadarChange.class;
            case Skynet.msgType -> Skynet.class;
            case SnitchHit.msgType -> SnitchHit.class;
            case WorldJoinChat.msgType -> WorldJoinChat.class;
            default -> null;
        };
    }
}
