package gjum.minecraft.civ.synapse.mod;

import gjum.minecraft.civ.synapse.common.Pos;
import gjum.minecraft.civ.synapse.common.Util;
import gjum.minecraft.civ.synapse.common.observations.Action;
import gjum.minecraft.civ.synapse.common.observations.ObservationImpl;
import gjum.minecraft.civ.synapse.common.observations.accountpos.PearlTransport;
import gjum.minecraft.civ.synapse.common.observations.accountpos.SnitchHit;
import gjum.minecraft.civ.synapse.common.observations.game.BastionChat;
import gjum.minecraft.civ.synapse.common.observations.game.BrandNew;
import gjum.minecraft.civ.synapse.common.observations.game.CombatEndChat;
import gjum.minecraft.civ.synapse.common.observations.game.CombatTagChat;
import gjum.minecraft.civ.synapse.common.observations.game.GroupChat;
import gjum.minecraft.civ.synapse.common.observations.game.PearlLocation;
import gjum.minecraft.civ.synapse.common.observations.game.PearledChat;
import gjum.minecraft.civ.synapse.common.observations.game.WorldJoinChat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChatHandler {
    public static final Pattern PEARL_LOCATION_PATTERN = Pattern.compile(
        "^(?:Your pearl is )?held by (?<holder>[ _a-zA-Z0-9]+) at (?:(?<world>\\S+) )?(?<x>-?\\d+)[, ]+(?<y>-?\\d+)[, ]+(?<z>-?\\d+).*",
        Pattern.CASE_INSENSITIVE
    );
    public static final Pattern PEARL_BROADCAST_PATTERN = Pattern.compile(
        "^(?:\\[(?<group>[^]]+)] ?)?The pearl of (?<prisoner>\\S+) is held by (?<holder>[ _a-zA-Z0-9]+) \\[x?:?(?<x>-?\\d+)[, ]+y?:?(?<y>-?\\d+)[, ]+z?:?(?<z>-?\\d+) (?<world>[\\S]+)]",
        Pattern.CASE_INSENSITIVE
    );

    public static final Pattern SNITCH_HIT_PATTERN = Pattern.compile(
        "^\\s*\\*\\s+([A-Za-z0-9_]{2,16})\\s+(entered|logged out|logged in) (?:in |to )?snitch at (\\S*) \\[(?:(\\S+)\\s)?\\s*(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)].*",
        Pattern.CASE_INSENSITIVE
    );
    public static final Pattern SNITCH_HOVER_PATTERN = Pattern.compile(
        "^(?i)\\s*Location:\\s*\\[(.+?) (-?\\d+) (-?\\d+) (-?\\d+)]\\s*Group:\\s*(\\S+?)\\s*Type:\\s*(Entry|Logging)\\s*(?:(?:Hours to cull|Cull):\\s*(\\d+\\.\\d+)h?)?\\s*(?:Previous name:\\s*(\\S+?))?\\s*(?:Name:\\s*(\\S+?))?\\s*",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );

    public static final Pattern BASTION_INFO_NONE_PATTERN = Pattern.compile(
        "^No Bastion Block.*",
        Pattern.CASE_INSENSITIVE
    );
    public static final Pattern BASTION_INFO_FRIENDLY_PATTERN = Pattern.compile(
        "^A Bastion Block prevents others from building.*",
        Pattern.CASE_INSENSITIVE
    );
    public static final Pattern BASTION_INFO_HOSTILE_PATTERN = Pattern.compile(
        "^A Bastion Block prevents you building.*",
        Pattern.CASE_INSENSITIVE
    );
    public static final Pattern BASTION_REMOVED_BLOCK_PATTERN = Pattern.compile(
        "^Bastion removed block.*",
        Pattern.CASE_INSENSITIVE
    );
    public static final Pattern BASTION_REMOVED_BOAT_PATTERN = Pattern.compile(
        "^Boat blocked by bastion.*",
        Pattern.CASE_INSENSITIVE
    );

    public static final Pattern COMBAT_TAG_PATTERN = Pattern.compile(
        "^You have engaged in(?:to)? combat with (?<account>[A-Za-z0-9_]{2,16}).*",
        Pattern.CASE_INSENSITIVE
    );
    public static final Pattern COMBAT_END_PATTERN = Pattern.compile(
        "^You are no longer (?:in )?combat(?: ?tagged)?.*",
        Pattern.CASE_INSENSITIVE
    );
    public static final Pattern PEARLED_PATTERN = Pattern.compile(
        "^You've been bound to an? (?<pearlType>exile|prison)? *pearl by (?<account>[A-Za-z0-9_]{2,16}).*",
        Pattern.CASE_INSENSITIVE
    );

    public static final Pattern GROUP_CHAT_PATTERN = Pattern.compile(
        "^\\[(?<group>\\S+)\\] (?<account>[A-Za-z0-9_]{2,16}): (?<message>.*)$",
        Pattern.CASE_INSENSITIVE
    );
    public static final Pattern LOCAL_CHAT_PATTERN = Pattern.compile(
        "^<(?<account>[A-Za-z0-9_]{2,16})>:? +(?<message>.*)$",
        Pattern.CASE_INSENSITIVE
    );
    public static final Pattern PRIVATE_CHAT_PATTERN = Pattern.compile(
        "^(?<direction>From|To) (?<account>[A-Za-z0-9_]{2,16}): (?<message>.*)$",
        Pattern.CASE_INSENSITIVE
    );
    public static final Pattern BRAND_NEW_PATTERN = Pattern.compile(
        "^(?<account>[A-Za-z0-9_]{2,16}) is brand new.*",
        Pattern.CASE_INSENSITIVE
    );

    public static @Nullable ObservationImpl observationFromChat(
        final @NotNull Component originalMessage
    ) {
        final String legacyFormattedMessage = McUtil.asLegacy(originalMessage);
        final ObservationImpl observation = observationFromChatInternal(originalMessage, legacyFormattedMessage);
        if (observation != null) {
            observation.setMessagePlain(legacyFormattedMessage);
        }
        return observation;
    }

    private static @Nullable ObservationImpl observationFromChatInternal(
        final @NotNull Component originalMessage,
        @NotNull String legacyFormattedMessage
    ) {
        legacyFormattedMessage = legacyFormattedMessage.replaceAll("§r", "").trim();
        if (legacyFormattedMessage.startsWith("§bJoined world: ")) {
            // CivRealms world announcement
            final String world = legacyFormattedMessage
                    .split(": ", 2)[1]
                    .replaceAll("§.", "")
                    .split(" ", 2)[0];
            LiteModSynapse.instance.onJoinedWorldFromChat(world);
            return new WorldJoinChat(
                McUtil.getSelfAccount(),
                world
            );
        }

        final String msg = McUtil.fullySanitiseString(legacyFormattedMessage).trim();

        final Matcher snitchHitMatcher = SNITCH_HIT_PATTERN.matcher(msg);
        if (snitchHitMatcher.matches()) {
            final String account = snitchHitMatcher.group(1);
            final Action action = Action.fromString(snitchHitMatcher.group(2).toLowerCase());
            final String snitch = snitchHitMatcher.group(3);
            final String world = snitchHitMatcher.group(4);
            final int x = Integer.parseInt(snitchHitMatcher.group(5));
            final int y = Integer.parseInt(snitchHitMatcher.group(6));
            final int z = Integer.parseInt(snitchHitMatcher.group(7));
            String group = null;
            String type = null;
            final String hover = hoverTextFromMessage(originalMessage);
            if (hover != null) {
                final Matcher hoverMatcher = SNITCH_HOVER_PATTERN.matcher(hover);
                if (hoverMatcher.matches()) {
                    group = hoverMatcher.group(5);
                    type = hoverMatcher.group(6);
                }
            }
            return new SnitchHit(
                McUtil.getSelfAccount(),
                account,
                new Pos(x, y, z),
                world,
                action,
                snitch,
                group,
                type
            );
        }

        final Matcher pearlLocationMatcher = PEARL_LOCATION_PATTERN.matcher(msg);
        if (pearlLocationMatcher.matches()) {
            final String prisoner = McUtil.getSelfAccount();
            final String holder = pearlLocationMatcher.group("holder");
            final int x = Integer.parseInt(pearlLocationMatcher.group("x"));
            final int y = Integer.parseInt(pearlLocationMatcher.group("y"));
            final int z = Integer.parseInt(pearlLocationMatcher.group("z"));
            final String world = Util.getMatchGroupOrNull("world", pearlLocationMatcher);
            return makePearlLocationOrTransport(
                McUtil.getSelfAccount(),
                new Pos(x, y, z),
                world,
                prisoner,
                holder
            );
        }

        final Matcher pearlBroadcastMatcher = PEARL_BROADCAST_PATTERN.matcher(msg);
        if (pearlBroadcastMatcher.matches()) {
            final String prisoner = pearlBroadcastMatcher.group("prisoner");
            final String holder = pearlBroadcastMatcher.group("holder");
            final int x = Integer.parseInt(pearlBroadcastMatcher.group("x"));
            final int y = Integer.parseInt(pearlBroadcastMatcher.group("y"));
            final int z = Integer.parseInt(pearlBroadcastMatcher.group("z"));
            final String world = Util.getMatchGroupOrNull("world", pearlBroadcastMatcher);
            return makePearlLocationOrTransport(
                McUtil.getSelfAccount(),
                new Pos(x, y, z),
                world,
                prisoner,
                holder
            );
        }

        final Matcher groupChatMatcher = GROUP_CHAT_PATTERN.matcher(msg);
        if (groupChatMatcher.matches()) {
            final String group = groupChatMatcher.group("group");
            final String account = groupChatMatcher.group("account");
            final String message = groupChatMatcher.group("message").trim();
            return new GroupChat(
                McUtil.getSelfAccount(),
                group,
                account,
                message
            );
        }

        final Matcher localChatMatcher = LOCAL_CHAT_PATTERN.matcher(msg);
        if (localChatMatcher.matches()) {
            final String account = localChatMatcher.group("account");
            final String message = localChatMatcher.group("message").trim();
            return new GroupChat(
                McUtil.getSelfAccount(),
                null,
                account,
                message
            );
        }

        final Matcher combatTagMatcher = COMBAT_TAG_PATTERN.matcher(msg);
        if (combatTagMatcher.matches()) {
            final String account = combatTagMatcher.group("account");
            return new CombatTagChat(
                McUtil.getSelfAccount(),
                account
            );
        }

        final Matcher combatEndMatcher = COMBAT_END_PATTERN.matcher(msg);
        if (combatEndMatcher.matches()) {
            return new CombatEndChat(
                McUtil.getSelfAccount()
            );
        }

        final Matcher pearledMatcher = PEARLED_PATTERN.matcher(msg);
        if (pearledMatcher.matches()) {
            final String account = pearledMatcher.group("account");
            final String pearlType = pearledMatcher.group("pearlType");
            return new PearledChat(
                McUtil.getSelfAccount(),
                account,
                pearlType
            );
        }

        final Matcher bastionInfoNoneMatcher = BASTION_INFO_NONE_PATTERN.matcher(msg);
        if (bastionInfoNoneMatcher.matches()) {
            final Pos pos = Util.mapNonNull(McUtil.getLookedAtBlockPos(5), McUtil::pos);
            return new BastionChat(
                McUtil.getSelfAccount(),
                pos,
                LiteModSynapse.instance.worldName,
                BastionChat.State.NONE,
                BastionChat.Source.INFO
            );
        }

        final Matcher bastionInfoFriendlyMatcher = BASTION_INFO_FRIENDLY_PATTERN.matcher(msg);
        if (bastionInfoFriendlyMatcher.matches()) {
            final Pos pos = Util.mapNonNull(McUtil.getLookedAtBlockPos(5), McUtil::pos);
            return new BastionChat(
                McUtil.getSelfAccount(),
                pos,
                LiteModSynapse.instance.worldName,
                BastionChat.State.FRIENDLY,
                BastionChat.Source.INFO
            );
        }

        final Matcher bastionInfoHostileMatcher = BASTION_INFO_HOSTILE_PATTERN.matcher(msg);
        if (bastionInfoHostileMatcher.matches()) {
            final Pos pos = Util.mapNonNull(McUtil.getLookedAtBlockPos(5), McUtil::pos);
            return new BastionChat(
                McUtil.getSelfAccount(),
                pos,
                LiteModSynapse.instance.worldName,
                BastionChat.State.HOSTILE,
                BastionChat.Source.INFO
            );
        }

        final Matcher bastionRemovedBlockMatcher = BASTION_REMOVED_BLOCK_PATTERN.matcher(msg);
        if (bastionRemovedBlockMatcher.matches()) {
            final Pos pos = Util.mapNonNull(McUtil.getLookedAtBlockPos(5), McUtil::pos);
            return new BastionChat(
                McUtil.getSelfAccount(),
                pos,
                LiteModSynapse.instance.worldName,
                BastionChat.State.HOSTILE,
                BastionChat.Source.BLOCK
            );
        }

        final Matcher bastionRemovedBoatMatcher = BASTION_REMOVED_BOAT_PATTERN.matcher(msg);
        if (bastionRemovedBoatMatcher.matches()) {
            final Pos pos = Util.mapNonNull(McUtil.getLookedAtBlockPos(5), McUtil::pos);
            return new BastionChat(
                McUtil.getSelfAccount(),
                pos,
                LiteModSynapse.instance.worldName,
                BastionChat.State.HOSTILE,
                BastionChat.Source.BOAT
            );
        }

        final Matcher brandNewMatcher = BRAND_NEW_PATTERN.matcher(msg);
        if (brandNewMatcher.matches()) {
            final String account = brandNewMatcher.group("account");
            return new BrandNew(
                McUtil.getSelfAccount(),
                account
            );
        }

        // XXX match other chat messages - any that contain account or pos, so they can be formatted and sent to teammates

        return null;
    }

    private static @NotNull ObservationImpl makePearlLocationOrTransport(
        final String witness,
        final Pos pos,
        final String world,
        final String prisoner,
        final String holder
    ) {
        if (PearlLocation.isPlayerHolder(holder)) {
            return new PearlTransport(witness, pos, world, prisoner, holder);
        }
        else {
            return new PearlLocation(witness, pos, world, prisoner, holder);
        }
    }

    private static @Nullable String hoverTextFromMessage(
        final @NotNull Component message
    ) {
        final HoverEvent hoverEvent = McUtil.findFirstHoverEvent(message);
        if (hoverEvent == null) {
            return null;
        }
        final Component content = hoverEvent.getValue(HoverEvent.Action.SHOW_TEXT);
        if (content == null) {
            return null;
        }
        final String plainContent = McUtil.fullySanitiseComponent(content).trim();
        if (plainContent.isEmpty()) {
            return null;
        }
        return plainContent;
    }
}
