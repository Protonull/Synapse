package gjum.minecraft.civ.synapse.mod.integrations;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import gjum.minecraft.civ.synapse.mod.FloatColor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VoxelMapHelper {
    public static void createWaypoint(
		final @NotNull MultiWaypoint waypoint
	) {
		if (!IntegrationHelpers.isVoxelMapPresent()) {
			return;
		}
        waypoint.vmWaypoint = new Waypoint(
			waypoint.getName(),
			waypoint.pos.x,
			waypoint.pos.y,
			waypoint.pos.z,
			waypoint.isVisible(),
			waypoint.color.r,
			waypoint.color.g,
			waypoint.color.b,
			waypoint.vmImage.toString(),
			waypoint.world,
			new TreeSet<>()
		);
        //waypoint.vmWaypoint.dimensions.add(waypoint.getDimension()); // TODO: Fix dimension
		VoxelConstants.getVoxelMapInstance().getWaypointManager().addWaypoint(waypoint.vmWaypoint);
    }

    public static void updateWaypoint(
		final @NotNull MultiWaypoint waypoint
	) {
		if (!IntegrationHelpers.isVoxelMapPresent()) {
			return;
		}
        if (waypoint.vmWaypoint == null) {
			createWaypoint(waypoint);
		}
        if (waypoint.vmWaypoint == null) {
			return;
		}
        waypoint.vmWaypoint.name = waypoint.getName();
        waypoint.vmWaypoint.x = waypoint.pos.x;
        waypoint.vmWaypoint.y = waypoint.pos.y;
        waypoint.vmWaypoint.z = waypoint.pos.z;
        waypoint.vmWaypoint.red = waypoint.color.r;
        waypoint.vmWaypoint.green = waypoint.color.g;
        waypoint.vmWaypoint.blue = waypoint.color.b;
        waypoint.vmWaypoint.enabled = waypoint.isVisible();
        waypoint.vmWaypoint.imageSuffix = waypoint.vmImage.toString();

        // add if it got deleted or it was somehow not already added
        // iterate reverse to avoid checking old waypoints
        final ArrayList<Waypoint> renderedWaypoints = VoxelConstants.getVoxelMapInstance().getWaypointManager().getWaypoints();
        for (int i = renderedWaypoints.size() - 1; i >= 0; i--) {
            if (renderedWaypoints.get(i) == waypoint.vmWaypoint) {
                return;
            }
        }
		VoxelConstants.getVoxelMapInstance().getWaypointManager().addWaypoint(waypoint.vmWaypoint);
    }

    public static void deleteWaypoint(
		final @NotNull MultiWaypoint waypoint
	) {
		if (!IntegrationHelpers.isVoxelMapPresent()) {
			return;
		}
        if (waypoint.vmWaypoint == null) {
			return;
		}
		VoxelConstants.getVoxelMapInstance().getWaypointManager().deleteWaypoint(waypoint.vmWaypoint);
        waypoint.vmWaypoint = null;
    }

    public static void deleteWaypoint(@NotNull Waypoint waypoint) {
		if (!IntegrationHelpers.isVoxelMapPresent()) {
			return;
		}
		VoxelConstants.getVoxelMapInstance().getWaypointManager().deleteWaypoint(waypoint);
    }

    /**
     * Set any arg to null to match any corresponding value.
     */
    public static Collection<Waypoint> getWaypointsByNameImgColor(@Nullable Pattern namePattern, @Nullable VmImage image, @Nullable FloatColor color) {
		if (!IntegrationHelpers.isVoxelMapPresent()) {
			return List.of();
		}
        return VoxelConstants.getVoxelMapInstance().getWaypointManager().getWaypoints().stream().filter(p ->
                (image == null || image.toString().equals(p.imageSuffix)
                ) && (color == null || (color.r == p.red && color.g == p.green && color.b == p.blue)
                ) && (namePattern == null || namePattern.matcher(p.name).matches())
        ).collect(Collectors.toList());
    }

	/** @deprecated Use {@link IntegrationHelpers#isVoxelMapPresent()} instead! */
	@Deprecated
    public static boolean isVoxelMapActive() {
        return IntegrationHelpers.isVoxelMapPresent();
    }
}
