package gjum.minecraft.civ.synapse.mod.features;

import gjum.minecraft.civ.synapse.mod.Standing;
import gjum.minecraft.civ.synapse.mod.SynapseMod;
import gjum.minecraft.civ.synapse.mod.config.HudConfig;
import gjum.minecraft.civ.synapse.mod.config.SynapseConfig;
import java.awt.Color;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class SynapseHud {
    public static volatile int HEALTH_POT_COUNT = 0;

    public static volatile Component NEARBY_HOSTILES_TEXT = Component.empty();
    public static volatile int NEARBY_HOSTILES_COLOUR = Color.GRAY.getRGB();

    public static volatile Component NEARBY_FRIENDLIES_TEXT = Component.empty();
    public static volatile int NEARBY_FRIENDLIES_COLOUR = Color.GRAY.getRGB();

    public static volatile Component NEARBY_PLAYERS_TEXT = Component.empty();
    public static volatile int NEARBY_PLAYERS_COLOUR = Color.GRAY.getRGB();

    public static void renderHud(
        final @NotNull GuiGraphics guiGraphics,
        final @NotNull DeltaTracker delta
    ) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ChatScreen) {
            final int chatBoxHeight = minecraft.gui.getChat().getHeight();
            if (chatBoxHeight > minecraft.screen.height / 2) {
                return;
            }
        }
        final HudConfig hudConfig = SynapseConfig.HANDLER.instance().hudConfig;
        int y = (minecraft.getWindow().getGuiScaledHeight() - 10 * 8) / 2;
        if (hudConfig.showHealthPotCount) {
            guiGraphics.drawStringWithBackdrop(
                minecraft.font,
                Component.literal(HEALTH_POT_COUNT + " hpots"),
                1,
                y += 10,
                0, // TODO: This probably needs a revisit
                Color.MAGENTA.getRGB()
            );
            y += 10;
        }
        if (hudConfig.showNearbyHostileCount) {
            guiGraphics.drawStringWithBackdrop(
                minecraft.font,
                NEARBY_HOSTILES_TEXT,
                1,
                y += 10,
                0, // TODO: This probably needs a revisit
                NEARBY_HOSTILES_COLOUR
            );
        }
        if (hudConfig.showNearbyFriendlyCount) {
            guiGraphics.drawStringWithBackdrop(
                minecraft.font,
                NEARBY_FRIENDLIES_TEXT,
                1,
                y += 10,
                0, // TODO: This probably needs a revisit
                NEARBY_FRIENDLIES_COLOUR
            );
        }
        if (hudConfig.showNearbyPlayerCount) {
            guiGraphics.drawStringWithBackdrop(
                minecraft.font,
                NEARBY_PLAYERS_TEXT,
                1,
                y += 10,
                0, // TODO: This probably needs a revisit
                NEARBY_PLAYERS_COLOUR
            );
        }
        y += 10;
    }


    public static void calculateNearbyPlayers() {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        int nearbyHostiles = 0, nearbyFriendlies = 0, nearbyPlayers = 0;
        for (final AbstractClientPlayer player : level.players()) {
            if (player == minecraft.player) {
                continue;
            }
            nearbyPlayers++;
            switch (((Standing.Holder) player).synapse$getStanding()) {
                case HOSTILE -> nearbyHostiles++;
                case FRIENDLY -> nearbyFriendlies++;
            }
        }
        SynapseHud.NEARBY_HOSTILES_TEXT = Component.literal(nearbyHostiles + " hostiles near");
        SynapseHud.NEARBY_HOSTILES_COLOUR = nearbyHostiles == 0 ? Color.GRAY.getRGB() : SynapseMod.getStandingColor(Standing.HOSTILE).getRGB();

        SynapseHud.NEARBY_FRIENDLIES_TEXT= Component.literal(nearbyFriendlies + " friendlies near");
        SynapseHud.NEARBY_FRIENDLIES_COLOUR = nearbyFriendlies == 0 ? Color.GRAY.getRGB() : SynapseMod.getStandingColor(Standing.FRIENDLY).getRGB();

        SynapseHud.NEARBY_PLAYERS_TEXT= Component.literal(nearbyPlayers + " total near");
        SynapseHud.NEARBY_PLAYERS_COLOUR = nearbyPlayers == 0 ? Color.GRAY.getRGB() : SynapseMod.getStandingColor(Standing.UNSET).getRGB();
    }
}
