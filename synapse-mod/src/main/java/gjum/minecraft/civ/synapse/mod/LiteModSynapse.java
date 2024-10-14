package gjum.minecraft.civ.synapse.mod;

import static gjum.minecraft.civ.synapse.common.Util.printErrorRateLimited;
import static gjum.minecraft.civ.synapse.mod.ObservationFormatter.addCoordClickEvent;
import static gjum.minecraft.civ.synapse.mod.ObservationFormatter.formatObservationStatic;

import gjum.minecraft.civ.synapse.common.observations.AccountObservation;
import gjum.minecraft.civ.synapse.common.observations.Observation;
import gjum.minecraft.civ.synapse.common.observations.ObservationImpl;
import gjum.minecraft.civ.synapse.common.observations.PlayerTracker;
import gjum.minecraft.civ.synapse.common.observations.accountpos.AccountPosObservation;
import gjum.minecraft.civ.synapse.common.observations.accountpos.PlayerState;
import gjum.minecraft.civ.synapse.common.observations.accountpos.RadarChange;
import gjum.minecraft.civ.synapse.common.observations.accountpos.SnitchHit;
import gjum.minecraft.civ.synapse.common.observations.game.CombatTagChat;
import gjum.minecraft.civ.synapse.common.observations.game.GroupChat;
import gjum.minecraft.civ.synapse.common.observations.game.PearlLocation;
import gjum.minecraft.civ.synapse.common.observations.game.Skynet;
import gjum.minecraft.civ.synapse.mod.config.AccountsConfig;
import gjum.minecraft.civ.synapse.mod.config.GlobalConfig;
import gjum.minecraft.civ.synapse.mod.config.PersonsConfig;
import gjum.minecraft.civ.synapse.mod.config.ServerConfig;
import gjum.minecraft.civ.synapse.mod.integrations.WaypointManager;
import gjum.minecraft.civ.synapse.mod.integrations.combatradar.CombatRadarHelpers;
import java.io.File;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LiteModSynapse {
    public static final String MOD_NAME = "Synapse";

    public static final String waypointCommandJourneyMap = "/jm wpedit [name:%s, x:%s, y:%s, z:%s]";
    public static final String waypointCommandVoxelMap = "/newWaypoint name:%s, x:%s, y:%s, z:%s";

    public static double closeDistance = 64;

    @Nullable
    public String gameAddress;
    private long loginTime = 0;
    @Nullable
    public String worldName = null;

    @NotNull
    public GlobalConfig config = new GlobalConfig();
    @NotNull
    public AccountsConfig accountsConfig = new AccountsConfig();
    @Nullable
    public ServerConfig serverConfig;
    @Nullable
    public PersonsConfig personsConfig;
    @Nullable
    public WaypointManager waypointManager;

    @NotNull
    private PlayerTracker playerTracker = new PlayerTrackerIngame(null);
    private long lastSync = 0;

    public static LiteModSynapse instance;

    private static final File modConfigDir = new File(Minecraft.getInstance().gameDirectory, MOD_NAME);

    public LiteModSynapse() {
        instance = this;
    }

    public String getName() {
        return MOD_NAME;
    }

    public String getVersion() {
        return "Development_Build";
    }

    public void init(File configPath) {
        accountsConfig.load(new File(modConfigDir, "accounts.txt"));
        accountsConfig.saveLater(null);

        // enabled by default on this server; but don't override existing config
        final File civRealmsConfigDir = new File(modConfigDir, "servers/civrealms.com");
        if (civRealmsConfigDir.mkdirs()) {
            new PersonsConfig().saveLater(new File(civRealmsConfigDir, "persons.json"));
        }

        //comms.connect();
    }

    public boolean isConnectedToGame() {
        return loginTime > 0 && gameAddress != null;
    }

    public boolean isServerEnabled() {
        return serverConfig != null && serverConfig.isEnabled();
    }

    public void setServerEnabled(boolean enabled) {
        if (serverConfig == null && enabled) {
            loadServerRelatedConfigs(true);
        }
        if (serverConfig != null) serverConfig.setEnabled(enabled);
        checkModActive();
    }

    public boolean isModActive() {
        return config.isModEnabled() && isServerEnabled();
    }

    public void checkModActive() {
        if (!config.isModEnabled()) {
            onModDeactivated();
            return;
        }
        if (serverConfig == null) {
            loadServerRelatedConfigs(false);
        }
        if (!isModActive()) {
            onModDeactivated();
            return;
        }

        // TODO only call onModActivated if it was not active before
        onModActivated();
    }

    private void onModActivated() {
        if (Objects.equals(playerTracker.gameAddress, gameAddress)) {
            return; // mod is already active
        }

        playerTracker = new PlayerTrackerIngame(gameAddress);
        if (waypointManager != null) waypointManager.updateAllWaypoints();
    }

    private void onModDeactivated() {
        if (waypointManager != null) waypointManager.updateAllWaypoints();
    }

    private void loadServerRelatedConfigs(boolean create) {
        try {
            if (gameAddress == null) return;

            if (personsConfig != null
                    && waypointManager != null
            ) return;

            final File serverConfigDir = getServerConfigDir(gameAddress, create);
            if (serverConfigDir == null) return;

            final CombatRadarHelpers combatRadarHelper = new CombatRadarHelpers();
            waypointManager = new WaypointManager();

            personsConfig = new PersonsConfig();
            personsConfig.getPersonsRegistry().registerChangeHandler(combatRadarHelper);
            personsConfig.getPersonsRegistry().registerChangeHandler(waypointManager);
            personsConfig.load(new File(serverConfigDir, "persons.json"));
            personsConfig.saveLater(null);
        } catch (Throwable e) {
            printErrorRateLimited(e);
            personsConfig = null;
            waypointManager = null;
        }
    }

    /**
     * If `create` is false and no directory matches, return null.
     * Otherwise, reuse existing directory or create it.
     * Allows omitting 25565 default port.
     */
    @Nullable
    private static File getServerConfigDir(@NotNull String gameAddress, boolean create) {
        final String[] addressTries = {
                gameAddress,
                gameAddress.endsWith(":25565")
                        ? gameAddress.replaceFirst(":25565$", "")
                        : gameAddress + ":25565"};
        final File serversConfigsDir = new File(modConfigDir, "servers/");
        for (String addressTry : addressTries) {
            final File serverConfigDir = new File(serversConfigsDir,
                    addressTry.replace(":", " "));
            if (serverConfigDir.isDirectory()) {
                return serverConfigDir;
            }
        }
        // no matching config directory exists
        if (create) {
            final File serverConfigDir = new File(serversConfigsDir,
                    gameAddress.replace(":", " "));
            serverConfigDir.mkdirs();
            return serverConfigDir;
        } else {
            return null;
        }
    }

    // TODO: Uncomment
//    @Override
//    public void onTick(Minecraft minecraft, float partialTicks, boolean inGame, boolean isGameTick) {
//        try {
//            final boolean noGuiOpen = minecraft.currentScreen == null;
//            final boolean handleKeyPresses = inGame && noGuiOpen;
//            if (handleKeyPresses) {
//                if (SynapseMod.CHAT_POS_KEYBIND.isDown()) {
//                    final Pos pos = McUtil.getEntityPosition(Minecraft.getInstance().player);
//                    Minecraft.getInstance().displayGuiScreen(new GuiChat(String.format(
//                            "[x:%s y:%s z:%s name:%s]",
//                            pos.x, pos.y, pos.z,
//                            McUtil.getSelfAccount())));
//                }
//            }
//            if (waypointManager != null && inGame && isGameTick) {
//                waypointManager.onTick();
//            }
//            if (lastSync < System.currentTimeMillis() - config.getSyncInterval()) {
//                lastSync = System.currentTimeMillis();
//                syncComms();
//            }
//        } catch (Throwable e) {
//            printErrorRateLimited(e);
//        }
//    }

    @Nullable
    public PersonsRegistry getPersonsRegistry() {
        if (personsConfig == null) return null;
        return personsConfig.getPersonsRegistry();
    }

    @NotNull
    public PlayerTracker getPlayerTracker() {
        return playerTracker;
    }

    /**
     * null means unknown observation type; caller should show original message in that case.
     */
    @Nullable
    public String getObservationFormat(@NotNull Observation observation) {
        if (observation instanceof SnitchHit) return config.getSnitchHitFormat();
        if (observation instanceof RadarChange) return config.getRadarFormat();
        if (observation instanceof Skynet) return config.getSkynetFormat();
        if (observation instanceof PearlLocation) return config.getPearlLocationFormat();
        if (observation instanceof CombatTagChat) return config.getCombatTagFormat();
        if (observation instanceof GroupChat) return config.getGroupChatFormat();
        return null; // unknown observation type; use original message
    }

    /**
     * @param fmtStr If null, it is guessed with getObservationFormat()
     */
    public @Nullable MutableComponent formatObservationWithVisibility(
        String fmtStr,
        final @NotNull Observation observation,
        final MutableComponent originalMsg
    ) {
        try {
            final GlobalConfig.VisibilityFormat visibilityFormat = config.getVisibilityFormat(observation);
            switch (visibilityFormat) {
                case HIDDEN:
                    return null;
                case ORIGINAL:
                    return originalMsg;
                case FORMATTED:
                    break;
                default:
                    throw new IllegalStateException("Invalid VisibilityFormat: " + visibilityFormat);
            }

            final ChatFormatting color = getChatColor(observation);
            if (color == null) {
                return null; // invisible at this urgency level/config
            }

            if (fmtStr == null) {
                fmtStr = getObservationFormat(observation);
            }

            // XXX use {format} stuff
            final MutableComponent formatted;
            if (fmtStr != null) {
                formatted = formatObservationStatic(fmtStr, observation);
                formatted.withStyle(color);
            }
            else if (originalMsg != null) {
                formatted = originalMsg;
            }
            else {
                return null; // drop remote message with unknown format
            }

            final String waypointCommandFormat = config.isUseVoxelMap() ? waypointCommandVoxelMap
                : config.isUseJourneyMap() ? waypointCommandJourneyMap
                : null;
            addCoordClickEvent(formatted, observation, waypointCommandFormat);

            return formatted;
        }
        catch (final Throwable e) {
            printErrorRateLimited(e);
            return originalMsg;
        }
    }

    /**
     * null = invisible/hide = don't show message
     */
    // XXX get rid of method: use {format} stuff
    public @Nullable ChatFormatting getChatColor(
        final @NotNull Observation observation
    ) {
        final Visibility visibility = getObservationVisibility(observation);
        return switch (visibility) {
            case ALERT -> ChatFormatting.RED;
            case DULL -> ChatFormatting.GRAY;
            case HIDE -> null;
            default -> ChatFormatting.WHITE;
        };
    }

    @NotNull
    public Visibility getObservationVisibility(@NotNull Observation observation) {
        if (!(observation instanceof AccountObservation accObs)) return Visibility.SHOW;
        final Standing standing = getStanding(accObs.getAccount());
        final boolean isOurs = observation.getWitness().equals(McUtil.getSelfAccount());
        if (!isOurs) {
            return Visibility.HIDE;
        }
        if (!config.matchesStandingFilter(standing, config.getStandingFilter(observation))) {
            return Visibility.HIDE;
        }
        final boolean isClose = isClose(accObs);
        return config.getChatVisibility(isClose, standing);
    }

    private boolean isClose(@NotNull AccountObservation observation) {
        // TODO: Uncomment
//        final EntityPlayer playerEntity = Minecraft.getInstance().world.getPlayerEntityByName(observation.getAccount());
//        boolean isClose = playerEntity != null;
//        final EntityPlayerSP self = Minecraft.getInstance().player;
//        if (self != null) {
//            Pos pos = null;
//            if (playerEntity != null) {
//                pos = McUtil.getEntityPosition(playerEntity);
//            } else if (observation instanceof AccountPosObservation) {
//                pos = ((AccountPosObservation) observation).getPos();
//            }
//            if (pos != null) isClose = closeDistance * closeDistance > pos.distanceSq(self.posX, self.posY, self.posZ);
//        }
//        return isClose;
        return false;
    }

    @NotNull
    public ChatFormatting getDistanceColor(int distance) {
        if (distance < closeDistance) return ChatFormatting.GOLD;
        if (distance < 500) return ChatFormatting.YELLOW;
        if (distance < 1000) return ChatFormatting.WHITE;
        return ChatFormatting.GRAY;
    }

    public void handleObservation(@NotNull Observation obs) {
        handleChatObservation(obs, null);
    }

    public void handleChatObservation(@NotNull Observation obs, @Nullable Component originalChat) {
        final boolean isNew = getPlayerTracker().recordObservation(obs);

        if (obs instanceof PlayerState playerState) {
            // don't update waypoint if that player is on radar anyway
            if (waypointManager != null && null == McUtil.findFirstPlayerByName(Minecraft.getInstance().level, playerState.getAccount())) {
                try {
                    waypointManager.updateAccountLocation(playerState);
                } catch (Throwable e) {
                    printErrorRateLimited(e);
                }
            }
            return;
        }

        if (!isNew) return;

        // ignore skynet spam at login
        final boolean skynetIgnored = obs instanceof Skynet && loginTime + 1000 > System.currentTimeMillis();
        if (!skynetIgnored) {
            // TODO: Uncomment
//            try {
//                final ITextComponent formattedMsg = formatObservationWithVisibility(null, obs, originalChat);
//                if (formattedMsg != null) {
//                    Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessage(formattedMsg);
//                }
//            } catch (Throwable e) {
//                printErrorRateLimited(e);
//                if (originalChat != null) Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessage(originalChat);
//            }
        }

        if (obs instanceof AccountPosObservation apObs) {
            if (waypointManager != null) {
                try {
                    waypointManager.updateAccountLocation(apObs);
                } catch (Throwable e) {
                    printErrorRateLimited(e);
                }
            }
            return;
        }
        if (obs instanceof PearlLocation && waypointManager != null) {
            try {
                waypointManager.updatePearlLocation((PearlLocation) obs);
            } catch (Throwable e) {
                printErrorRateLimited(e);
            }
        }

//        if (McUtil.getSelfAccount().equals(obs.getWitness()) && comms != null) {
//            // TODO: Uncomment
////            comms.sendEncrypted(new JsonPacket(obs));
//        }
    }

    public void onJoinedWorldFromChat(String world) {
        this.worldName = world;
    }

    @NotNull
    public Standing getStanding(String account) {
        if (serverConfig == null) return Standing.UNSET;
        return serverConfig.getAccountStanding(account);
    }

    public Component getDisplayNameForAccount(@NotNull String accountName) {
        // TODO: Uncomment
//        accountName = accountName.replaceAll("§.", "");
//        if (!isModActive() || !config.isReplaceNamePlates()) return new TextComponentString(accountName);
//        if (getPersonsRegistry() == null || serverConfig == null) return new TextComponentString(accountName);
//        // set standing color even when person is unknown
//        final Standing standing = serverConfig.getAccountStanding(accountName);
//        final TextFormatting color = config.getStandingColor(standing);
//        final ITextComponent displayName = new TextComponentString(accountName)
//                .setStyle(new Style().setColor(color));
//        if (config.isShowPersonNextToAccount()) {
//            final Person person = getPersonsRegistry().personByAccountName(accountName);
//            if (person != null && !person.isMain(accountName)) {
//                displayName.appendSibling(new TextComponentString(" (" + person.getName() + ")")
//                        .setStyle(new Style().setColor(TextFormatting.GRAY)));
//            }
//        }
//        return displayName;
        return Component.empty();
    }

    @Nullable
    public Component handleChat(Component original) {
        try {
            if (!isModActive()) return original;
            ObservationImpl observation = ChatHandler.observationFromChat(original);
            if (observation != null) {
                // TODO: Uncomment
                //handleChatObservation(observation, original);
                return null; // formatted+printed by observation handler, if new and visible
            }
        } catch (Throwable e) {
            printErrorRateLimited(e);
        }
        return original;
    }

    private void handlePacketPlayerListItem(ClientboundPlayerInfoUpdatePacket packet) {
        // TODO: Uncomment
//        for (SPacketPlayerListItem.AddPlayerData entry : packet.getEntries()) {
//            final GameProfile profile = entry.getProfile();
//            final UUID playerUuid = profile.getId();
//            if (packet.getAction() == SPacketPlayerListItem.Action.ADD_PLAYER) {
//                if (profile.getName() == null || profile.getName().isEmpty()) continue;
//                if (profile.getName().contains("~")) continue; // dummy entry by TabListPlus
//
//                final NetworkPlayerInfo existingPlayerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(profile.getId());
//                if (existingPlayerInfo != null) continue; // already logged in
//
//                final String accountName = profile.getName().replaceAll("§.", "").trim();
//                accountsConfig.addAccount(accountName);
//
//                // TODO detect combat logger by comparing playerUuid
//                Action action = Action.LOGIN;
//                if (entry.getGameMode() == GameType.NOT_SET) {
//                    action = Action.CTLOG;
//                }
//
//                final Skynet observation = new Skynet(
//                        McUtil.getSelfAccount(), playerUuid, accountName, action, entry.getGameMode().getID());
//                handleObservation(observation);
//            } else if (packet.getAction() == SPacketPlayerListItem.Action.REMOVE_PLAYER) {
//                final NetworkPlayerInfo playerInfo = Minecraft.getInstance().getConnection()
//                        .getPlayerInfo(playerUuid);
//                if (playerInfo == null) continue;
//                final GameProfile existingProfile = playerInfo.getGameProfile();
//                if (existingProfile.getName() == null) continue;
//                // ignore dummy entries by TabListPlus
//                if (existingProfile.getName().contains("~")) continue;
//                final String accountName = existingProfile.getName().replaceAll("§.", "").trim();
//                if (accountName.isEmpty()) continue;
//
//                final Skynet observation = new Skynet(
//                        McUtil.getSelfAccount(), playerUuid, accountName, Action.LOGOUT, playerInfo.getGameType().getID());
//                handleObservation(observation);
//            }
//        }
    }

    public static void playSound(@NotNull String soundName, @NotNull UUID playerUuid) {
        // TODO: Uncomment
//        if (soundName.isEmpty() || "none".equalsIgnoreCase(soundName)) return;
//        float playerPitch = 0.5F + 1.5F * (new Random(playerUuid.hashCode())).nextFloat();
//        final ResourceLocation resource = new ResourceLocation(soundName);
//        Minecraft.getInstance().player.playSound(new SoundEvent(resource), 1.0F, playerPitch);
    }

    public long getLoginTime() {
        return loginTime;
    }
}
