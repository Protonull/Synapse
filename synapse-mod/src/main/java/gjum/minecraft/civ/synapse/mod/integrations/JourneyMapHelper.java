package gjum.minecraft.civ.synapse.mod.integrations;

import static gjum.minecraft.civ.synapse.mod.LiteModSynapse.MOD_NAME;
import static gjum.minecraft.civ.synapse.mod.McUtil.isJourneyMapLoaded;
import static gjum.minecraft.civ.synapse.mod.integrations.JourneyMapPlugin.jmApi;

import gjum.minecraft.civ.synapse.mod.McUtil;
import journeymap.api.v2.common.waypoint.WaypointFactory;
import org.jetbrains.annotations.NotNull;

public class JourneyMapHelper {
    public static void createWaypoint(@NotNull MultiWaypoint waypoint) {
        if (!isJourneyMapLoaded()) return;
        waypoint.jmWaypoint = WaypointFactory.createClientWaypoint(
            MOD_NAME,
            McUtil.blockPos(waypoint.pos),
            (String) null, //waypoint.getDimension(), // TODO: Fix dimension
            false
        );
        waypoint.jmWaypoint.setColor(waypoint.color.getHex());
        //waypoint.jmWaypoint.setPrimaryDimension(waypoint.getDimension()); // TODO: Fix dimension
        waypoint.jmWaypoint.setEnabled(waypoint.isVisible());
        waypoint.jmWaypoint.setPersistent(false);
        try {
            jmApi.addWaypoint(MOD_NAME, waypoint.jmWaypoint);
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void updateWaypoint(@NotNull MultiWaypoint waypoint) {
        if (!isJourneyMapLoaded()) return;
        if (waypoint.jmWaypoint == null) createWaypoint(waypoint);
        if (waypoint.jmWaypoint == null) return; // could not create
        waypoint.jmWaypoint.setName(waypoint.getName());
        //waypoint.jmWaypoint.setPrimaryDimension(waypoint.getDimension()); // TODO: Fix dimension
        waypoint.jmWaypoint.setPos(waypoint.pos.x, waypoint.pos.y, waypoint.pos.z);
        waypoint.jmWaypoint.setColor(waypoint.color.getHex());
        waypoint.jmWaypoint.setEnabled(waypoint.isVisible());
        try {
            jmApi.addWaypoint(MOD_NAME, waypoint.jmWaypoint);
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void deleteWaypoint(@NotNull MultiWaypoint waypoint) {
        if (!isJourneyMapLoaded()) return;
        if (waypoint.jmWaypoint == null) return;
        jmApi.removeWaypoint(MOD_NAME, waypoint.jmWaypoint);
        waypoint.jmWaypoint = null;
    }
}
