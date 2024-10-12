package gjum.minecraft.civ.synapse.mod.mixins;

import gjum.minecraft.civ.synapse.common.Util;
import gjum.minecraft.civ.synapse.mod.features.SynapseHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Calculates how many splash health-potions the player has anytime the inventory is updated, to prevent it being
 * calculated every single frame.
 */
@Mixin(ClientPacketListener.class)
public abstract class HealthPotCounterMixin {
    @Inject(
        method = "handleContainerContent",
        at = @At("TAIL")
    )
    protected void synapse$handleSetContent(
        final @NotNull ClientboundContainerSetContentPacket packet,
        final @NotNull CallbackInfo ci
    ) {
        SynapseHud.HEALTH_POT_COUNT = countSplashHealthPotions(Minecraft.getInstance().player);
    }

    @Inject(
        method = "handleContainerSetSlot",
        at = @At("TAIL")
    )
    protected void synapse$handleSetSlot(
        final @NotNull ClientboundContainerSetSlotPacket packet,
        final @NotNull CallbackInfo ci
    ) {
        SynapseHud.HEALTH_POT_COUNT = countSplashHealthPotions(Minecraft.getInstance().player);
    }

    @Unique
    private int countSplashHealthPotions(
        final LocalPlayer player
    ) {
        if (player == null) { // Should never happen, but just in case
            return 0;
        }
        final Inventory playerInventory = player.getInventory();
        return Util.countMatches(playerInventory.items, this::isSplashHealthPotion)
            + Util.countMatches(playerInventory.offhand, this::isSplashHealthPotion);
    }

    @Unique
    private boolean isSplashHealthPotion(
        final @NotNull ItemStack item
    ) {
        if (item.getItem() != Items.SPLASH_POTION) {
            return false;
        }
        final PotionContents potion = item.get(DataComponents.POTION_CONTENTS);
        if (potion == null) {
            return false;
        }
        return potion.is(Potions.HEALING) || potion.is(Potions.STRONG_HEALING);
    }
}
