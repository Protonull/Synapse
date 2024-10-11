package gjum.minecraft.civ.synapse.mod;

import static gjum.minecraft.civ.synapse.common.Util.lowerCaseSet;
import static gjum.minecraft.civ.synapse.common.Util.printErrorRateLimited;
import static gjum.minecraft.civ.synapse.common.Util.sortedUniqListIgnoreCase;
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
import gjum.minecraft.civ.synapse.common.observations.instruction.FocusAnnouncement;
import gjum.minecraft.civ.synapse.mod.config.AccountsConfig;
import gjum.minecraft.civ.synapse.mod.config.GlobalConfig;
import gjum.minecraft.civ.synapse.mod.config.PersonsConfig;
import gjum.minecraft.civ.synapse.mod.config.ServerConfig;
import gjum.minecraft.civ.synapse.mod.integrations.WaypointManager;
import gjum.minecraft.civ.synapse.mod.integrations.combatradar.CombatRadarHelpers;
import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
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

    @NotNull
    private Collection<String> focusedAccountNames = Collections.emptyList();

    @Nullable
    public Screen gui = null;

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
        // TODO: Uncomment
//        LiteLoader.getInstance().writeConfig(this);

        // move old config if it exists
        File oldConfigDir = new File(Minecraft.getInstance().gameDirectory, "Hydrate");
        if (oldConfigDir.exists() && !modConfigDir.exists()) {
            oldConfigDir.renameTo(modConfigDir);
        }

        modConfigDir.mkdirs();
        config.load(new File(modConfigDir, "config.json"));
        config.saveLater(null);
        accountsConfig.load(new File(modConfigDir, "accounts.txt"));
        accountsConfig.saveLater(null);

        // enabled by default on this server; but don't override existing config
        final File civRealmsConfigDir = new File(modConfigDir, "servers/civrealms.com");
        if (civRealmsConfigDir.mkdirs()) {
            new ServerConfig().saveLater(new File(civRealmsConfigDir, "server.json"));
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
        final ClientLevel world = Minecraft.getInstance().level;
        if (world != null) {
            for (final AbstractClientPlayer player : world.players()) {
                player.setGlowingTag(false);
            }
        }
        if (waypointManager != null) waypointManager.updateAllWaypoints();
    }

    private void loadServerRelatedConfigs(boolean create) {
        try {
            if (gameAddress == null) return;

            if (serverConfig != null
                    && personsConfig != null
                    && waypointManager != null
            ) return;

            final File serverConfigDir = getServerConfigDir(gameAddress, create);
            if (serverConfigDir == null) return;

            final CombatRadarHelpers combatRadarHelper = new CombatRadarHelpers();
            waypointManager = new WaypointManager();

            serverConfig = new ServerConfig();
            serverConfig.registerChangeHandler(combatRadarHelper);
            serverConfig.registerChangeHandler(waypointManager);
            serverConfig.load(new File(serverConfigDir, "server.json"));
            serverConfig.saveLater(null);

            personsConfig = new PersonsConfig();
            personsConfig.getPersonsRegistry().registerChangeHandler(combatRadarHelper);
            personsConfig.getPersonsRegistry().registerChangeHandler(waypointManager);
            personsConfig.load(new File(serverConfigDir, "persons.json"));
            personsConfig.saveLater(null);
        } catch (Throwable e) {
            printErrorRateLimited(e);
            serverConfig = null;
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

    public void showGuiAndRemember(@Nullable Screen gui) {
        // TODO: Uncomment
//        if (gui == null || gui instanceof GuiRoot) {
//            this.gui = gui;
//        } // else: some Minecraft gui; don't retain it
//        if (gui instanceof GuiRoot) ((GuiRoot) gui).rebuild();
        Minecraft.getInstance().setScreen(gui);
    }

    public void openLastGui() {
        // TODO: Uncomment
//        if (gui == null) gui = new MainGui(null);
        Minecraft.getInstance().setScreen(gui);
    }

    // TODO: Uncomment
//    @Override
//    public void onJoinGame(INetHandler netHandler, SPacketJoinGame joinGamePacket, ServerData serverData, RealmsServer realmsServer) {
//        try {
//            final String prevAddress = gameAddress;
//            gameAddress = Minecraft.getMinecraft().getCurrentServerData()
//                    .serverIP.split("/")[0]
//                    .toLowerCase();
//            loginTime = System.currentTimeMillis();
//
//            if (!gameAddress.equals(prevAddress)) {
//                serverConfig = null;
//                personsConfig = null;
//                waypointManager = null;
//                focusedAccountNames.clear();
//            }
//            checkModActive();
//        } catch (Throwable e) {
//            printErrorRateLimited(e);
//        }
//    }

    // TODO: Uncomment
//    @Override
//    public void onTick(Minecraft minecraft, float partialTicks, boolean inGame, boolean isGameTick) {
//        try {
//            final boolean noGuiOpen = minecraft.currentScreen == null;
//            final boolean handleKeyPresses = inGame && noGuiOpen;
//            if (handleKeyPresses) {
//                if (SynapseMod.TOGGLE_ENABLED_KEYBIND.isDown()) {
//                    config.setModEnabled(!config.isModEnabled());
//                }
//                if (SynapseMod.SET_FOCUS_ENTITY_KEYBIND.isDown()) {
//                    focusEntityUnderCrosshair();
//                }
//                if (SynapseMod.CHAT_POS_KEYBIND.isDown()) {
//                    final Pos pos = McUtil.getEntityPosition(Minecraft.getInstance().player);
//                    Minecraft.getInstance().displayGuiScreen(new GuiChat(String.format(
//                            "[x:%s y:%s z:%s name:%s]",
//                            pos.x, pos.y, pos.z,
//                            McUtil.getSelfAccount())));
//                }
//                if (SynapseMod.OPEN_GUI_KEYBIND.isDown()) {
//                    openLastGui();
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

    private void syncComms() {
        if (Minecraft.getInstance().level == null) return;
        /*
        boolean flushEveryPacket = false;
        for (EntityPlayer player : Minecraft.getInstance().world.playerEntities) {
            if (player == Minecraft.getInstance().player) continue; // send more info for self at the end
            // TODO don't send if pos didn't change
            comms.sendEncrypted(new JsonPacket(new PlayerState(McUtil.getSelfAccount(),
                    player.getName(), McUtil.getEntityPosition(player), worldName)
            ), flushEveryPacket);
        }*/
        final PlayerState selfState = new PlayerState(McUtil.getSelfAccount(),
                McUtil.getSelfAccount(), McUtil.getEntityPosition(Minecraft.getInstance().player), worldName);
        //selfState.heading = headingFromYawDegrees(Minecraft.getInstance().player.rotationYawHead);
        //selfState.health = getHealth();
        //selfState.hpotCount = getNumHealthPots();
        // TODO send combat tag end, min armor dura
        // TODO: Uncomment
//        comms.sendEncrypted(new JsonPacket(selfState), true);
    }

    // TODO: Uncomment
//    @Override
//    public void onPostRender(float partialTicks) {
//        for (AbstractClientPlayer player : Minecraft.getInstance().level.players()) {
//            renderDecorators(player, partialTicks);
//        }
//    }

    public void renderDecorators(
        AbstractClientPlayer entity,
        float partialTicks
    ) {
        // TODO: Uncomment
//        try {
//            if (!isModActive()) return;
//            if (legalToRenderDecorations(entity) && shouldRenderPlayerDecoration(entity)) {
//                try {
//                    prepareRenderPlayerDecorations(entity, partialTicks);
//                    PlayerTeam team = null;
//                    FloatColor color = null;
//                    boolean computedTeam = false;
//                    if (config.isPlayerMiddleHoop()) {
//                        if (!computedTeam) {
//                            team = config.getStandingTeam(getStanding(entity.getName()));
//                            if (team != null) color = FloatColor.fromChatFormatting(team.getColor());
//                            computedTeam = true;
//                        }
//                        if (team != null) {
//                            renderHoop(entity, 0.5, 1, partialTicks, color);
//                        }
//                    }
//                    if (config.isPlayerOuterHoops()) {
//                        if (!computedTeam) {
//                            team = config.getStandingTeam(getStanding(entity.getName()));
//                            if (team != null) color = FloatColor.fromChatFormatting(team.getColor());
//                            computedTeam = true;
//                        }
//                        if (team != null) {
//                            renderHoop(entity, 0.3, 0.01, partialTicks, color);
//                            renderHoop(entity, 0.3, 1.8, partialTicks, color);
//                        }
//                    }
//                    if (config.isPlayerBox()) {
//                        if (!computedTeam) {
//                            team = config.getStandingTeam(getStanding(entity.getName()));
//                            if (team != null) color = FloatColor.fromChatFormatting(team.getColor());
//                            computedTeam = true;
//                        }
//                        if (team != null) {
//                            renderBox(entity, partialTicks, color);
//                        }
//                    }
//                } finally {
//                    resetRenderPlayerDecorations();
//                }
//            }
//        } catch (Throwable e) {
//            printErrorRateLimited(e);
//        }
    }

    // TODO: Uncomment
//    private boolean legalToRenderDecorations(EntityPlayer player) {
//        return !player.isInvisible() && !player.isSneaking();
//    }

    // TODO: Uncomment
//    private void prepareRenderPlayerDecorations(@NotNull Entity entity, float partialTicks) {
//        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
//        GlStateManager.disableTexture2D();
//        GlStateManager.disableLighting();
//        GlStateManager.enableBlend();
//        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
//        GL11.glEnable(GL11.GL_LINE_SMOOTH);
//        GlStateManager.glLineWidth(config.getPlayerLineWidth());
//
//        GlStateManager.disableDepth();
//        GlStateManager.depthMask(false);
//    }

    // TODO: Uncomment
//    private void resetRenderPlayerDecorations() {
//        GlStateManager.enableTexture2D();
//        GlStateManager.enableLighting();
//        GlStateManager.disableBlend();
//        GlStateManager.enableDepth();
//        GlStateManager.depthMask(true);
//        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
//    }

    // TODO: Uncomment
//    private void renderBox(Entity entity, float partialTicks, FloatColor color) {
//        final EntityPlayerSP player = Minecraft.getInstance().player;
//        double playerX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTicks;
//        double playerY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTicks;
//        double playerZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTicks;
//
//        float entityX = (float) (entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) partialTicks);
//        float entityY = (float) (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partialTicks);
//        float entityZ = (float) (entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) partialTicks);
//
//        double renderX = entityX - playerX;
//        double renderY = entityY - playerY;
//        double renderZ = entityZ - playerZ;
//
//        final double halfWidth = entity.width / 2;
//        final AxisAlignedBB box = new AxisAlignedBB(
//                renderX - halfWidth, renderY, renderZ - halfWidth,
//                renderX + halfWidth, renderY + entity.height, renderZ + halfWidth);
//
//        RenderGlobal.drawSelectionBoundingBox(box, color.r, color.g, color.b, 1);
//    }

    // TODO: Uncomment
//    private void renderHoop(Entity entity, double radius, double yOffset, float partialTicks, FloatColor color) {
//        final EntityPlayerSP player = Minecraft.getInstance().player;
//        float playerX = (float) (player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTicks);
//        float playerY = (float) (player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTicks);
//        float playerZ = (float) (player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTicks);
//
//        float entityX = (float) (entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) partialTicks);
//        float entityY = (float) (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partialTicks);
//        float entityZ = (float) (entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) partialTicks);
//
//        try {
//            GlStateManager.pushMatrix();
//            GlStateManager.translate(entityX - playerX, entityY - playerY, entityZ - playerZ);
//
//            Tessellator tessellator = Tessellator.getInstance();
//            BufferBuilder bufferBuilder = tessellator.getBuffer();
//            bufferBuilder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
//            double theta = 0.19634954084936207D;
//            double c = Math.cos(theta);
//            double s = Math.sin(theta);
//            double x = radius;
//            double y = 0.0D;
//            for (int circleSegment = 0; circleSegment < 32; circleSegment++) {
//                bufferBuilder.pos(x, yOffset, y).color(color.r, color.g, color.b, 1f).endVertex();
//                double t = x;
//                x = c * x - s * y;
//                y = s * t + c * y;
//            }
//            tessellator.draw();
//        } finally {
//            GlStateManager.popMatrix();
//        }
//    }

    // TODO: Uncomment
//    private static boolean shouldRenderPlayerDecoration(Entity ent) {
//        if (ent != Minecraft.getMinecraft().getRenderViewEntity()) return true;
//        if (Minecraft.getMinecraft().gameSettings.thirdPersonView != 0) return true;
//        final GuiScreen screen = Minecraft.getMinecraft().currentScreen;
//        final boolean guiShowsPlayer = screen instanceof GuiInventory || screen instanceof GuiContainerCreative;
//        return guiShowsPlayer && Minecraft.getMinecraft().getRenderManager().playerViewY == 180.0F;
//        return false;
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
        if (observation instanceof FocusAnnouncement) return config.getFocusAnnouncementFormat();
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
        if (!(observation instanceof AccountObservation)) return Visibility.SHOW;
        final AccountObservation accObs = (AccountObservation) observation;
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
    public static Color getStandingColor(@Nullable Standing standing) {
        // TODO: Uncomment
//        final ChatFormatting standingFmt = LiteModSynapse.instance.config.getStandingColor(standing);
//        return FloatColor.fromChatFormatting(standingFmt).toColor();
        return Color.CYAN;
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

        if (obs instanceof PlayerState) {
            final PlayerState playerState = (PlayerState) obs;
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

        if (obs instanceof AccountPosObservation) {
            final AccountPosObservation apObs = (AccountPosObservation) obs;
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

    public boolean isFocusedAccount(@NotNull String account) {
        if (focusedAccountNames.isEmpty()) return false;
        return focusedAccountNames.contains(account.toLowerCase());
    }

    private void focusEntityUnderCrosshair() {
        // TODO: Uncomment
//        try {
//            if (!isModActive()) return;
//            if (Minecraft.getInstance().objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) return;
//            Entity entityHit = Minecraft.getInstance().objectMouseOver.entityHit;
//            if (entityHit == null) { // do long trace only if default short trace didn't hit yet
//                Vec3d traceStart = EntityUtilities.getPositionEyes(Minecraft.getInstance().player, Minecraft.getInstance().getRenderPartialTicks());
//                final Method method = EntityUtilities.class.getDeclaredMethod("rayTraceEntities", Entity.class, double.class, float.class, double.class, Vec3d.class);
//                method.setAccessible(true);
//                final Object trace = method.invoke(null, Minecraft.getInstance().player, 64.0, Minecraft.getInstance().getRenderPartialTicks(), 64.0, traceStart);
//                final Field entityField = trace.getClass().getDeclaredField("entity");
//                entityField.setAccessible(true);
//                entityHit = (Entity) entityField.get(trace);
//            }
//            if (!(entityHit instanceof EntityPlayer)) return;
//            final EntityPlayer player = (EntityPlayer) entityHit;
//            // allow re-sending focus message
////            if (isFocusedAccount(player.getName())) return;
//            announceFocusedAccount(player.getName());
//        } catch (Throwable e) {
//            printErrorRateLimited(e);
//        }
    }

    public void setFocusedAccountNames(@Nullable Collection<String> accounts) {
        final Collection<String> impactedAccounts = new ArrayList<>(focusedAccountNames);
        focusedAccountNames = lowerCaseSet(accounts);
        impactedAccounts.addAll(focusedAccountNames);
        final PersonsRegistry personsRegistry = getPersonsRegistry();
        if (personsRegistry != null) {
            personsRegistry.propagateLargeChange(impactedAccounts.stream()
                    .map(personsRegistry::personByAccountName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()));
        }
        if (waypointManager != null) waypointManager.updateAllWaypoints();
    }

    public void announceFocusedAccount(@NotNull String account) {
        announceFocusedAccounts(Collections.singletonList(account));
    }

    public void announceFocusedAccounts(@NotNull Collection<String> focusedAccounts) {
        focusedAccounts = sortedUniqListIgnoreCase(focusedAccounts);
        setFocusedAccountNames(focusedAccounts);
        // TODO: Uncomment
//        comms.sendEncrypted(new JsonPacket(new FocusAnnouncement(
//                McUtil.getSelfAccount(), focusedAccounts)));
        Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Focusing: " + String.join(" ", focusedAccounts)));
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

    private void handlePacketSpawnPlayer(Object packet) {
        // TODO: Find out what the player-spawn packet is
        // TODO: Uncomment
//        final UUID playerUuid = packet.getUniqueId();
//        final String accountName = Minecraft.getInstance().getConnection().getPlayerInfo(playerUuid)
//                .getGameProfile().getName().replaceAll("§.", "");
//        final Pos pos = new Pos(
//                MathHelper.floor(packet.getX()),
//                MathHelper.floor(packet.getY()),
//                MathHelper.floor(packet.getZ()));
//        final RadarChange observation = new RadarChange(
//                McUtil.getSelfAccount(), accountName, pos, worldName, Action.APPEARED);
//        try {
//            if (config.isPlayRadarSound()) {
//                final Standing standing = mapNonNull(serverConfig, sc ->
//                        sc.getAccountStanding(accountName));
//                final String soundName = config.getStandingSound(standing);
//                if (soundName != null) playSound(soundName, playerUuid);
//            }
//        } catch (Throwable e) {
//            printErrorRateLimited(e);
//        }
//        handleObservation(observation);
    }

    private void handlePacketDestroyEntities(ClientboundRemoveEntitiesPacket packet) {
        // TODO: Uncomment
//        for (int eid : packet.getEntityIDs()) {
//            final Entity entity = Minecraft.getInstance().world.getEntityByID(eid);
//            if (!(entity instanceof EntityPlayer)) continue;
//            final EntityPlayer player = (EntityPlayer) entity;
//            final RadarChange observation = new RadarChange(
//                    McUtil.getSelfAccount(), player.getName(), McUtil.getEntityPosition(player), worldName, Action.DISAPPEARED);
//            handleObservation(observation);
//        }
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

    public void handleCommsConnected() {
        Minecraft.getInstance().execute(() -> {
            // XXX store msg for gui, update gui if open
        });
    }

    public void handleCommsEncryptionSuccess(String message) {
        Minecraft.getInstance().execute(() -> {

            // XXX store msg for gui, update gui if open
        });
    }

    public void handleCommsDisconnected(Throwable cause) {
        Minecraft.getInstance().execute(() -> {
            // XXX store msg for gui, update gui if open
        });
    }

    // TODO: brb

    public void handleCommsJson(Object payload) {
        Minecraft.getInstance().execute(() -> {
            if (payload instanceof Observation) {
                handleObservation((Observation) payload);
            }
        });
    }
}
