package gjum.minecraft.civ.synapse.mod;

import gjum.minecraft.civ.synapse.common.Pos;
import gjum.minecraft.civ.synapse.common.Util;
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
import java.util.Objects;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ObservationFormatter {
    public static MutableComponent formatObservationStatic(
        final @NotNull String fmtStr,
        final @NotNull Observation observation
    ) {
        final MutableComponent root = Component.empty();
        MutableComponent component = root;
        boolean isKey = false;
        for (String s : fmtStr.split("%")) {
            if (isKey) {
                component.append(formatKey(observation, s));
            }
            else if (!s.isEmpty()) {
                // TODO apply § formatting
                root.append(component = Component.literal(s));
            }
            isKey = !isKey;
        }
        return root;
    }

    private static AccountPosObservation lastAPObsCache = null;
    private static String lastAPObsCacheAccount = null;
    private static long lastAPObsCacheTime = 0;

    private static AccountPosObservation lookUpLastAPObsInPlayerTracker(
        @NotNull String account
    ) {
        account = account.toLowerCase();
        if (lastAPObsCacheTime < System.currentTimeMillis() - 1000) {
            lastAPObsCache = null; // outdated
        }
        if (lastAPObsCache != null && account.equals(lastAPObsCacheAccount)) {
            return lastAPObsCache;
        }
        final AccountPosObservation apobs = LiteModSynapse.instance.getPlayerTracker()
                .getMostRecentPosObservationForAccount(account);
        if (apobs != null) {
            lastAPObsCache = apobs;
            lastAPObsCacheAccount = account;
            lastAPObsCacheTime = System.currentTimeMillis();
        }
        return apobs;
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    private static @NotNull Component formatKey(
        final @NotNull Observation observation,
        final @NotNull String key
    ) {
        if (key.isEmpty()) {
            return Component.literal("%");
        }
        final Component formatted = switch (observation) {
            case final CombatTagChat combatTag -> switch (key) {
                case "CALLER" -> Component.literal(combatTag.witness);
                default -> null;
            };
            case final FocusAnnouncement focused -> switch (key) {
                case "CALLER" -> Component.literal(focused.witness);
                default -> null;
            };
            case final GroupChat groupChat -> switch (key) {
                case "GROUP" -> Component.literal(Objects.requireNonNullElse(groupChat.group, "<local>"));
                case "MESSAGE" -> Component.literal(groupChat.message).withStyle(ChatFormatting.WHITE);
                default -> null;
            };
            case final PearlLocation pearl -> switch (key) {
                case "PRISONER" -> setStandingColor(pearl.prisoner, Component.literal(pearl.prisoner));
                case "HOLDER" -> {
                    if (pearl instanceof PearlTransport) {
                        yield setStandingColor(pearl.holder, Component.literal(pearl.holder));
                    }
                    else {
                        yield Component.literal(pearl.holder);
                    }
                }
                default -> null;
            };
            case final RadarChange radarChange -> switch (key) {
                case "ACTION" -> Component.literal(radarChange.action.name().toLowerCase());
                case "ACTIONSHORT" -> Component.literal(radarChange.action.shortName);
                default -> null;
            };
            case final Skynet skynet -> switch (key) {
                case "ACTION" -> Component.literal(skynet.action.name().toLowerCase());
                case "ACTIONSHORT" -> Component.literal(skynet.action.shortName);
                default -> null;
            };
            case final SnitchHit snitchHit -> switch (key) {
                case "SNITCH" -> Component.literal(snitchHit.snitchName);
                case "ACTION" -> Component.literal(String.valueOf(snitchHit.action).toLowerCase());
                case "ACTIONSHORT" -> Component.literal(Objects.requireNonNullElse(Util.mapNonNull(snitchHit.action, (a) -> a.shortName), "?"));
                case "GROUP" -> Component.literal(String.valueOf(snitchHit.group));
                case "TYPE" -> Component.literal(String.valueOf(snitchHit.snitchType));
                default -> null;
            };
            case final AccountPosObservation accountPosition -> formatAccountPosition(key, accountPosition);
            case final PosObservation position -> formatPosition(key, position);
            case final AccountObservation account -> switch (key) {
                case "ACCOUNT" -> setStandingColor(account.getAccount(), Component.literal(account.getAccount()));
                case "PERSON" -> setStandingColor(
                    account.getAccount(),
                    Component.literal(
                        Optional.ofNullable(LiteModSynapse.instance.getPersonsRegistry())
                            .map((registry) -> registry.personByAccountName(account.getAccount()))
                            .map(Person::getName)
                            .orElse(account.getAccount())
                    )
                );
                case "ACCOUNTPERSON" -> LiteModSynapse.instance.getDisplayNameForAccount(account.getAccount());
                default -> {
                    // ... even if observation isn't AccountPos, format last seen pos from PlayerTracker
                    final AccountPosObservation apobsFromTracker = lookUpLastAPObsInPlayerTracker(account.getAccount());
                    if (apobsFromTracker == null) {
                        yield null;
                    }
                    yield Optional.<Component>empty()
                        .or(() -> Optional.ofNullable(formatAccountPosition(key, apobsFromTracker)))
                        .or(() -> Optional.ofNullable(formatPosition(key, apobsFromTracker)))
                        .orElse(null);
                }
            };
            default -> null;
        };
        return Objects.requireNonNullElse(
            formatted,
            // unknown key or can't be used with this observation
            Component.literal("%" + key + "%")
        );
    }

    private static @Nullable Component formatPosition(
        final @NotNull String key,
        final @NotNull PosObservation observation
    ) {
        return switch (key) {
            case "XYZ" -> Component.literal("%d %d %d".formatted(
                observation.getPos().x,
                observation.getPos().y,
                observation.getPos().z
            ));
            case "X" -> Component.literal(String.valueOf(observation.getPos().x));
            case "Y" -> Component.literal(String.valueOf(observation.getPos().y));
            case "Z" -> Component.literal(String.valueOf(observation.getPos().z));
            case "WORLD" -> Component.literal(observation.getWorld());
            case "OFFWORLD" -> {
                if ("world".equals(observation.getWorld())) {
                    yield Component.empty();
                }
                yield Component.literal(observation.getWorld());
            }
            case "OTHERWORLD" -> {
                if (observation.getWorld().equals(LiteModSynapse.instance.worldName)) {
                    yield Component.empty();
                }
                yield Component.literal(observation.getWorld());
            }
            case "DISTANCE" -> {
                final Pos myPos = McUtil.getEntityPosition(Minecraft.getInstance().player);
                final int dx = myPos.x - observation.getPos().x;
                final int dz = myPos.z - observation.getPos().z;
                final int distance = (int) Math.sqrt(dx * dx + dz * dz);
                return Component
                    .literal(distance < 1000 ? distance + "m" : (distance / 1000) + "." + ((distance % 1000) / 100) + "km")
                    .withStyle(LiteModSynapse.instance.getDistanceColor(distance));
            }
            case "RELATIVE" -> {
                final Pos myPos = McUtil.getEntityPosition(Minecraft.getInstance().player);
                final Pos delta = observation.getPos().subtract(myPos);
                yield Component.literal(Util.headingFromDelta(delta.x, delta.y, delta.z));
            }
            default -> null;
        };
    }

    private static @Nullable Component formatAccountPosition(
        final @NotNull String key,
        final @NotNull AccountPosObservation observation
    ) {
        return switch (key) {
            case "HEADING" -> {
                final AccountPosObservation previousObs = LiteModSynapse.instance.getPlayerTracker()
                    .getLastObservationBeforeWithSignificantMove(observation);
                if (previousObs == null) {
                    yield Component.literal("?");
                }
                final Pos delta = observation.getPos().subtract(previousObs.getPos());
                yield Component.literal(Util.headingFromDelta(delta.x, delta.y, delta.z));
            }
            case "DISTANCEDELTA" -> {
                final AccountPosObservation previousObs = LiteModSynapse.instance.getPlayerTracker()
                    .getLastObservationBeforeWithSignificantMove(observation);
                if (previousObs == null) {
                    yield Component.empty();
                }
                final Pos myPos = McUtil.getEntityPosition(Minecraft.getInstance().player);
                final double distPrev = myPos.distance(previousObs.getPos());
                final double distNow = myPos.distance(observation.getPos());
                final double distDelta = distNow - distPrev;
                // TODO ratio-based distance significance check, to emulate angle-based check
                if (Math.abs(distDelta) < PlayerTracker.closeObservationDistance) {
                    yield Component.empty(); // undetermined; not significant change
                }
                yield Component.literal(distNow > distPrev ? "+" : "-");
            }
            default -> null;
        };
    }

    private static @NotNull Component setStandingColor(
        final @NotNull String account,
        final @NotNull MutableComponent component
    ) {
        final ServerConfig serverConfig = LiteModSynapse.instance.serverConfig;
        if (serverConfig != null) {
            return component.withStyle(
                LiteModSynapse.instance.config.getStandingColor(
                    serverConfig.getAccountStanding(account)
                )
            );
        }
        return component;
    }

    public static void addCoordClickEvent(
        final @NotNull MutableComponent component,
        final @NotNull Observation observation,
        final String waypointCommandFormat
    ) {
        if (waypointCommandFormat == null) {
            return;
        }
        if (!(observation instanceof final AccountPosObservation apo)) {
            return;
        }
        final Pos pos = apo.getPos();
        final String waypointCommand = waypointCommandFormat.formatted(
            apo.getAccount(),
            pos.x,
            pos.y,
            pos.z
        );
        component.getStyle().withClickEvent(new ClickEvent(
            ClickEvent.Action.RUN_COMMAND,
            waypointCommand
        ));
    }
}
