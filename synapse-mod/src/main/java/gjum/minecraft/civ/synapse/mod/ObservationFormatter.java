package gjum.minecraft.civ.synapse.mod;

import static gjum.minecraft.civ.synapse.common.Util.headingFromDelta;
import static gjum.minecraft.civ.synapse.common.Util.mapNonNull;
import static gjum.minecraft.civ.synapse.common.Util.nonNullOr;
import static gjum.minecraft.civ.synapse.mod.McUtil.getEntityPosition;
import static gjum.minecraft.civ.synapse.mod.McUtil.getMc;

import gjum.minecraft.civ.synapse.common.Pos;
import gjum.minecraft.civ.synapse.common.observations.AccountObservation;
import gjum.minecraft.civ.synapse.common.observations.Observation;
import gjum.minecraft.civ.synapse.common.observations.PlayerTracker;
import gjum.minecraft.civ.synapse.common.observations.PosObservation;
import gjum.minecraft.civ.synapse.common.observations.accountpos.AccountPosObservation;
import gjum.minecraft.civ.synapse.common.observations.accountpos.PearlTransport;
import gjum.minecraft.civ.synapse.common.observations.accountpos.RadarChange;
import gjum.minecraft.civ.synapse.common.observations.accountpos.SnitchHit;
import gjum.minecraft.civ.synapse.common.observations.game.CombatTagChat;
import gjum.minecraft.civ.synapse.common.observations.game.GroupChat;
import gjum.minecraft.civ.synapse.common.observations.game.PearlLocation;
import gjum.minecraft.civ.synapse.common.observations.game.Skynet;
import gjum.minecraft.civ.synapse.common.observations.instruction.FocusAnnouncement;
import gjum.minecraft.civ.synapse.mod.config.ServerConfig;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ObservationFormatter {
	public static ITextComponent formatObservationStatic(String fmtStr, Observation observation) {
		final ITextComponent root = new TextComponentString("");
		ITextComponent component = root;
		boolean isKey = false;
		for (String s : fmtStr.split("%")) {
			if (isKey) {
				component.appendSibling(formatKey(observation, s));
			} else if (!s.isEmpty()) {
				// TODO apply § formatting
				root.appendSibling(component = new TextComponentString(s));
			}
			isKey = !isKey;
		}
		return root;
	}

	private static AccountPosObservation lastAPObsCache = null;
	private static String lastAPObsCacheAccount = null;
	private static long lastAPObsCacheTime = 0;

	private static AccountPosObservation lookUpLastAPObsInPlayerTracker(@NotNull String account) {
		account = account.toLowerCase();
		if (lastAPObsCacheTime < System.currentTimeMillis() - 1000) lastAPObsCache = null; // outdated
		if (lastAPObsCache != null && account.equals(lastAPObsCacheAccount)) return lastAPObsCache;
		final AccountPosObservation apobs = LiteModSynapse.instance.getPlayerTracker()
				.getMostRecentPosObservationForAccount(account);
		if (apobs != null) {
			lastAPObsCache = apobs;
			lastAPObsCacheAccount = account;
			lastAPObsCacheTime = System.currentTimeMillis();
		}
		return apobs;
	}

	@NotNull
	private static ITextComponent formatKey(@NotNull Observation observation, @NotNull String key) {
		if ("".equals(key)) return new TextComponentString("%"); // escaped percent sign (%%)
		ITextComponent formatted;
		if (observation instanceof CombatTagChat) {
			formatted = formatKeyCombatTagChat(key, (CombatTagChat) observation);
			if (formatted != null) return formatted;
		}
		if (observation instanceof FocusAnnouncement) {
			formatted = formatKeyFocusInstruction(key, (FocusAnnouncement) observation);
			if (formatted != null) return formatted;
		}
		if (observation instanceof GroupChat) {
			formatted = formatKeyGroupChat(key, (GroupChat) observation);
			if (formatted != null) return formatted;
		}
		if (observation instanceof PearlLocation) {
			formatted = formatKeyPearlLocation(key, (PearlLocation) observation);
			if (formatted != null) return formatted;
		}
		if (observation instanceof RadarChange) {
			formatted = formatKeyRadarChange(key, (RadarChange) observation);
			if (formatted != null) return formatted;
		}
		if (observation instanceof Skynet) {
			formatted = formatKeyLoginout(key, (Skynet) observation);
			if (formatted != null) return formatted;
		}
		if (observation instanceof SnitchHit) {
			formatted = formatKeySnitchHit(key, (SnitchHit) observation);
			if (formatted != null) return formatted;
		}
		// preferably lookup pos/accountpos ...
		if (observation instanceof AccountPosObservation) {
			lastAPObsCache = null;
			formatted = formatKeyAccountPos(key, (AccountPosObservation) observation);
			if (formatted != null) return formatted;
		}
		if (observation instanceof PosObservation) {
			formatted = formatKeyPos(key, (PosObservation) observation);
			if (formatted != null) return formatted;
		}
		if (observation instanceof AccountObservation) {
			formatted = formatKeyAccount(key, (AccountObservation) observation);
			if (formatted != null) return formatted;
			// ... even if observation isn't AccountPos, format last seen pos from PlayerTracker
			final AccountPosObservation apobsFromTracker = lookUpLastAPObsInPlayerTracker(
					((AccountObservation) observation).getAccount());
			if (apobsFromTracker != null) {
				formatted = formatKeyAccountPos(key, apobsFromTracker);
				if (formatted != null) return formatted;
				formatted = formatKeyPos(key, apobsFromTracker);
				if (formatted != null) return formatted;
			}
		}
		// unknown key or can't be used with this observation
		return new TextComponentString("%" + key + "%");
	}

	@Nullable
	private static ITextComponent formatKeyAccount(@NotNull String key, @NotNull AccountObservation observation) {
		switch (key) {
			case "ACCOUNT": {
				return setStandingColor(observation.getAccount(), new TextComponentString(observation.getAccount()));
			}
			case "PERSON": {
				return setStandingColor(observation.getAccount(), new TextComponentString(nonNullOr(mapNonNull(mapNonNull(
						LiteModSynapse.instance.getPersonsRegistry(),
						r -> r.personByAccountName(observation.getAccount())),
						Person::getName),
						observation.getAccount())));
			}
			case "ACCOUNTPERSON": {
				return LiteModSynapse.instance.getDisplayNameForAccount(observation.getAccount());
			}
			default:
				return null;
		}
	}

	@Nullable
	private static ITextComponent formatKeyPos(@NotNull String key, @NotNull PosObservation observation) {
		switch (key) {
			case "XYZ":
				return new TextComponentString(String.format("%d %d %d",
						observation.getPos().x, observation.getPos().y, observation.getPos().z));
			case "X":
				return new TextComponentString(String.valueOf(observation.getPos().x));
			case "Y":
				return new TextComponentString(String.valueOf(observation.getPos().y));
			case "Z":
				return new TextComponentString(String.valueOf(observation.getPos().z));
			case "WORLD":
				return new TextComponentString(String.valueOf(observation.getWorld()));
			case "OFFWORLD":
				if ("world".equals(observation.getWorld())) {
					return new TextComponentString("");
				}
				return new TextComponentString(String.valueOf(observation.getWorld()));
			case "OTHERWORLD":
				if (observation.getWorld().equals(LiteModSynapse.instance.worldName)) {
					return new TextComponentString("");
				}
				return new TextComponentString(String.valueOf(observation.getWorld()));

			case "DISTANCE": {
				final Pos myPos = getEntityPosition(getMc().player);
				final int dx = myPos.x - observation.getPos().x;
				final int dz = myPos.z - observation.getPos().z;
				final int distance = (int) Math.sqrt(dx * dx + dz * dz);
				final String text;
				if (distance < 1000) text = distance + "m";
				else text = (distance / 1000) + "." + ((distance % 1000) / 100) + "km";
				final TextComponentString component = new TextComponentString(text);
				final TextFormatting color = LiteModSynapse.instance.getDistanceColor(distance);
				component.getStyle().setColor(color);
				return component;
			}
			case "RELATIVE": {
				final Pos myPos = getEntityPosition(getMc().player);
				final Pos delta = observation.getPos().subtract(myPos);
				return new TextComponentString(headingFromDelta(delta.x, delta.y, delta.z));
			}
			default:
				return null;
		}
	}

	@Nullable
	private static ITextComponent formatKeyAccountPos(@NotNull String key, @NotNull AccountPosObservation observation) {
		switch (key) {
			case "HEADING": {
				final AccountPosObservation prevObs = LiteModSynapse.instance.getPlayerTracker()
						.getLastObservationBeforeWithSignificantMove(observation);
				if (prevObs == null) return new TextComponentString("?");
				final Pos delta = observation.getPos().subtract(prevObs.getPos());
				return new TextComponentString(headingFromDelta(delta.x, delta.y, delta.z));
			}
			case "DISTANCEDELTA": {
				final AccountPosObservation prevObs = LiteModSynapse.instance.getPlayerTracker()
						.getLastObservationBeforeWithSignificantMove(observation);
				if (prevObs == null) return new TextComponentString("");
				final Pos myPos = getEntityPosition(getMc().player);
				final double distPrev = myPos.distance(prevObs.getPos());
				final double distNow = myPos.distance(observation.getPos());
				final double distDelta = distNow - distPrev;
				// TODO ratio-based distance significance check, to emulate angle-based check
				if (Math.abs(distDelta) < PlayerTracker.closeObservationDistance) {
					return new TextComponentString(""); // undetermined; not significant change
				}
				return new TextComponentString(distNow > distPrev ? "+" : "-");
			}
			default:
				return null;
		}
	}

	@Nullable
	public static ITextComponent formatKeyCombatTagChat(@NotNull String key, @NotNull CombatTagChat combatTag) {
		if ("CALLER".equals(key)) {
			return new TextComponentString(combatTag.witness);
		}
		return null;
	}

	@Nullable
	public static ITextComponent formatKeyFocusInstruction(@NotNull String key, @NotNull FocusAnnouncement focus) {
		if ("CALLER".equals(key)) {
			return new TextComponentString(focus.witness);
		}
		return null;
	}

	@Nullable
	public static ITextComponent formatKeyGroupChat(@NotNull String key, @NotNull GroupChat observation) {
		switch (key) {
			case "GROUP":
				return new TextComponentString(nonNullOr(observation.group, "<local>"));
			case "MESSAGE":
				return new TextComponentString(observation.message)
						.setStyle(new Style().setColor(TextFormatting.WHITE));
			default:
				return null;
		}
	}

	@Nullable
	public static ITextComponent formatKeyPearlLocation(@NotNull String key, @NotNull PearlLocation pearl) {
		if ("PRISONER".equals(key)) {
			return setStandingColor(pearl.prisoner,
					new TextComponentString(pearl.prisoner));
		}
		if ("HOLDER".equals(key)) {
			if (pearl instanceof PearlTransport) {
				return setStandingColor(pearl.holder,
						new TextComponentString(pearl.holder));
			} else {
				return new TextComponentString(pearl.holder);
			}
		}
		return null;
	}

	@Nullable
	public static ITextComponent formatKeyRadarChange(@NotNull String key, @NotNull RadarChange radar) {
		if ("ACTION".equals(key)) return new TextComponentString(radar.action.name().toLowerCase());
		if ("ACTIONSHORT".equals(key)) return new TextComponentString(radar.action.shortName);
		return null;
	}

	@Nullable
	public static ITextComponent formatKeyLoginout(@NotNull String key, @NotNull Skynet skynet) {
		if ("ACTION".equals(key)) return new TextComponentString(skynet.action.name().toLowerCase());
		if ("ACTIONSHORT".equals(key)) return new TextComponentString(skynet.action.shortName);
		return null;
	}

	@Nullable
	public static ITextComponent formatKeySnitchHit(@NotNull String key, @NotNull SnitchHit snitchHit) {
		switch (key) {
			case "SNITCH":
				return new TextComponentString(snitchHit.snitchName);
			case "ACTION":
				return new TextComponentString(String.valueOf(snitchHit.action).toLowerCase());
			case "ACTIONSHORT":
				return new TextComponentString(nonNullOr(mapNonNull(snitchHit.action, a -> a.shortName), "?"));
			case "GROUP":
				return new TextComponentString(String.valueOf(snitchHit.group));
			case "TYPE":
				return new TextComponentString(String.valueOf(snitchHit.snitchType));
			default:
				return null;
		}
	}

	@NotNull
	private static ITextComponent setStandingColor(
			@NotNull String account,
			@NotNull ITextComponent component
	) {
		final ServerConfig serverConfig = LiteModSynapse.instance.serverConfig;
		if (serverConfig != null) {
			// set standing color even when person is unknown
			final Standing standing = serverConfig.getAccountStanding(account);
			final TextFormatting color = LiteModSynapse.instance.config.getStandingColor(standing);
			component.getStyle().setColor(color);
		}
		return component;
	}

	public static void addCoordClickEvent(
			@NotNull ITextComponent component,
			@NotNull Observation observation,
			@Nullable String waypointCommandFormat
	) {
		if (waypointCommandFormat == null) return;
		if (!(observation instanceof AccountPosObservation)) return;
		final AccountPosObservation apo = ((AccountPosObservation) observation);
		final Pos pos = apo.getPos();
		final String waypointCommand = String.format(waypointCommandFormat,
				apo.getAccount(), pos.x, pos.y, pos.z);
		component.getStyle().setClickEvent(new ClickEvent(
				ClickEvent.Action.RUN_COMMAND, waypointCommand));
	}
}
