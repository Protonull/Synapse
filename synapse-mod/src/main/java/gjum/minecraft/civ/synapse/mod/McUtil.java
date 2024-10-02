package gjum.minecraft.civ.synapse.mod;

import gjum.minecraft.civ.synapse.common.Pos;
import gjum.minecraft.civ.synapse.mod.integrations.JourneyMapPlugin;
import java.util.Collection;
import java.util.function.Predicate;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public class McUtil {
	public static @NotNull Minecraft getMc() {
		return Minecraft.getInstance();
	}

	public static boolean isJourneyMapLoaded() {
		return FabricLoader.getInstance().isModLoaded("journeymap") && JourneyMapPlugin.jmApi != null;
	}

	/**
	 * does the rounding correctly for negative coordinates
	 */
	public static @NotNull Pos getEntityPosition(
		final @NotNull Entity entity
	) {
		return new Pos(
			Mth.floor(entity.getX()),
			Mth.floor(entity.getY()),
			Mth.floor(entity.getZ())
		);
	}

	public static @NotNull Pos pos(
		final @NotNull BlockPos pos
	) {
		return new Pos(
			pos.getX(),
			pos.getY(),
			pos.getZ()
		);
	}

	public static @NotNull BlockPos blockPos(
		final @NotNull Pos pos
	) {
		return new BlockPos(
			pos.x,
			pos.y,
			pos.z
		);
	}

	public static @NotNull String getDisplayNameFromTablist(
		final @NotNull PlayerInfo info
	) {
		final Component tabListName = info.getTabListDisplayName();
		if (tabListName != null) {
			return fullySanitiseComponent(tabListName);
		}
		return fullySanitiseString(info.getProfile().getName());
	}

	public static @NotNull String fullySanitiseComponent(
		final @NotNull Component component
	) {
		return fullySanitiseString(component.getString());
	}

	public static @NotNull String fullySanitiseString(
		final @NotNull String string
	) {
		return string.replaceAll("§.", "");
	}

	public static @NotNull String getSelfAccount() {
		final User account = Minecraft.getInstance().getUser();
		final ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection != null) {
			final PlayerInfo info = connection.getPlayerInfo(account.getProfileId());
			if (info != null) {
				return fullySanitiseString(info.getProfile().getName());
			}
		}
		final LocalPlayer player = Minecraft.getInstance().player;
		if (player != null) {
			return fullySanitiseComponent(player.getName());
		}
		return fullySanitiseString(account.getName());
	}

	public static int getNumHealthPots() {
		final Inventory playerInventory = Minecraft.getInstance().player.getInventory();
		return countMatches(playerInventory.items, McUtil::isHealthPot)
			+ countMatches(playerInventory.offhand, McUtil::isHealthPot);
	}

	public static <T> @Range(from = 0, to = Integer.MAX_VALUE) int countMatches(
		final @NotNull Collection<T> collection,
		final @NotNull Predicate<T> predicate
	) {
		int count = 0;
		for (final T element : collection) {
			if (predicate.test(element)) {
				count++;
			}
		}
		return count;
	}

	private static boolean isHealthPot(
		final @NotNull ItemStack item
	) {
		if (item.getItem() != Items.SPLASH_POTION) {
			return false;
		}
		final PotionContents potion = item.get(DataComponents.POTION_CONTENTS);
		if (potion == null) {
			return false;
		}
		return potion.is(Potions.HEALING);
	}

	public static @Nullable BlockPos getLookedAtBlockPos(
		final int reach
	) {
		throw new NotImplementedException("CivMC illegal");
	}
}
