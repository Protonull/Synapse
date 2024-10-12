package gjum.minecraft.civ.synapse.mod.features;

import gjum.minecraft.civ.synapse.mod.LiteModSynapse;
import gjum.minecraft.civ.synapse.mod.McUtil;
import gjum.minecraft.civ.synapse.mod.Standing;
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
    public static void renderHud(
        final @NotNull GuiGraphics guiGraphics,
        final @NotNull DeltaTracker delta
    ) {
        final Minecraft minecraft = Minecraft.getInstance();
        final boolean hasChatOpen = minecraft.screen instanceof ChatScreen;
        final int chatBoxHeight = minecraft.gui.getChat().getHeight();
        if (hasChatOpen && chatBoxHeight > minecraft.screen.height / 2) {
            return;
        }
        final int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int y = (screenHeight - 10 * 8) / 2;
        if (SynapseConfig.HANDLER.instance().hudShowHealthPotCount) {
            renderHealthPotCountHud(
                minecraft,
                guiGraphics,
                y += 10
            );
            y += 10;
        }
        if (SynapseConfig.HANDLER.instance().hudShowNearbyHostileCount) {
            renderPlayerCountHud(minecraft, guiGraphics, y += 10, "hostile", Standing.HOSTILE);
        }
        if (SynapseConfig.HANDLER.instance().hudShowNearbyFriendlyCount) {
            renderPlayerCountHud(minecraft, guiGraphics, y += 10, "friendly", Standing.FRIENDLY);
        }
        if (SynapseConfig.HANDLER.instance().hudShowNearbyPlayerCount) {
            renderPlayerCountHud(minecraft, guiGraphics, y += 10, "total", null);
        }
        y += 10;
    }

    private static void renderHealthPotCountHud(
        final @NotNull Minecraft minecraft,
        final @NotNull GuiGraphics guiGraphics,
        final int y
    ) {
        final long numHealthPots = McUtil.getNumHealthPots(); // TODO: Store this value somewhere. We should NOT be inspecting items on render.
        guiGraphics.drawStringWithBackdrop(
            minecraft.font,
            Component.literal(numHealthPots + " hpots"),
            1,
            y,
            0, // TODO: This probably needs a revisit
            Color.MAGENTA.getRGB()
        );
    }

    private static void renderPlayerCountHud(
        final @NotNull Minecraft minecraft,
        final @NotNull GuiGraphics guiGraphics,
        final int y,
        final @NotNull String text,
        final Standing standing
    ) {
        int numPlayers = getNumVisiblePlayersWithStanding(minecraft, standing);
        if (standing == Standing.HOSTILE) {
            numPlayers += getNumVisiblePlayersWithStanding(minecraft, Standing.FOCUS);
        }
        guiGraphics.drawStringWithBackdrop(
            minecraft.font,
            Component.literal(numPlayers + " " + text + " near"),
            1,
            y,
            0, // TODO: This probably needs a revisit
            numPlayers == 0
                ? Color.GRAY.getRGB()
                : LiteModSynapse.getStandingColor(standing).getRGB()
        );
    }

    private static int getNumVisiblePlayersWithStanding(
        final @NotNull Minecraft minecraft,
        final Standing standing
    ) {
        final ClientLevel level = minecraft.level;
        if (level == null) {
            return 0;
        }
        if (standing == null) {
            return Math.max(level.players().size() - 1, 0); // don't count the player itself
        }
        int count = 0;
        for (final AbstractClientPlayer player : level.players()) {
            if (player == minecraft.player) {
                continue;
            }
            if (LiteModSynapse.instance.getStanding(McUtil.fullySanitiseComponent(player.getName())) == standing) {
                count++;
            }
        }
        return count;
    }
}
