package gjum.minecraft.civ.synapse.mod.integrations;

import net.fabricmc.loader.api.FabricLoader;

public final class IntegrationHelpers {
    public static boolean isCombatRadarPresent() {
        return FabricLoader.getInstance().isModLoaded("combatradar");
    }

    public static boolean isVoxelMapPresent() {
        return FabricLoader.getInstance().isModLoaded("voxelmap");
    }
}
