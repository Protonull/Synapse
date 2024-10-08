package gjum.minecraft.civ.synapse.mod.integrations;

import static gjum.minecraft.civ.synapse.common.Util.printErrorRateLimited;
import static gjum.minecraft.civ.synapse.mod.integrations.MultiWaypoint.namePattern;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import gjum.minecraft.civ.synapse.common.Pos;
import gjum.minecraft.civ.synapse.common.observations.accountpos.AccountPosObservation;
import gjum.minecraft.civ.synapse.common.observations.game.PearlLocation;
import gjum.minecraft.civ.synapse.mod.FloatColor;
import gjum.minecraft.civ.synapse.mod.LiteModSynapse;
import gjum.minecraft.civ.synapse.mod.Person;
import gjum.minecraft.civ.synapse.mod.PersonChangeHandler;
import gjum.minecraft.civ.synapse.mod.Standing;
import gjum.minecraft.civ.synapse.mod.config.ServerConfig;
import java.util.Collection;
import java.util.HashMap;
import java.util.regex.Matcher;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WaypointManager implements PersonChangeHandler {
    private static VmImage accountImageVm = VmImage.person;
    private static final String accountPrefix = "";
    private static VmImage pearlImageVm = VmImage.record;
    private static final String pearlPrefix = "Pearl of ";
    private static FloatColor pearlColor = new FloatColor(.8f, 0, 1);

    private HashMap<String, MultiWaypoint> managedAccountWaypoints = new HashMap<>();
    private HashMap<String, MultiWaypoint> managedPearlWaypoints = new HashMap<>();

    private boolean needSynchronizeVoxelmapWaypoints = true;

    public void updateAccountLocation(@NotNull AccountPosObservation update) {
        final MultiWaypoint point = managedAccountWaypoints.computeIfAbsent(update.getAccount().toLowerCase(), accountLower -> {
            final MultiWaypoint point1 = new MultiWaypoint(update.getPos(), accountPrefix, update.getAccount());
            point1.setImage(accountImageVm);
            return point1;
        });
        point.setColor(getStandingColor(update.getAccount()));
        point.setPos(update.getPos());
    }

    public void updatePearlLocation(@NotNull PearlLocation update) {
        final MultiWaypoint point = managedPearlWaypoints.computeIfAbsent(update.prisoner.toLowerCase(), accountLower -> {
            final MultiWaypoint p = new MultiWaypoint(update.pos, pearlPrefix, update.prisoner);
            p.setImage(pearlImageVm);
            p.setColor(pearlColor);
            return p;
        });
        point.setPos(update.pos);
        // only show pearl when not carried by player
        point.setHiddenForOverlap(update.isPlayerHolder());
    }

    @NotNull
    private FloatColor getStandingColor(@NotNull String accountName) {
        Standing standing = null;
        final ServerConfig serverConfig = LiteModSynapse.instance.serverConfig;
        if (serverConfig != null) {
            standing = serverConfig.getAccountStanding(accountName);
        }
        final ChatFormatting standingColor = LiteModSynapse.instance.config.getStandingColor(standing);
        return FloatColor.fromChatFormatting(standingColor);
    }

    @NotNull
    private FloatColor getStandingColor(@NotNull Person person) {
        Standing standing = null;
        final ServerConfig serverConfig = LiteModSynapse.instance.serverConfig;
        if (serverConfig != null) {
            standing = serverConfig.getStanding(person);
        }
        final ChatFormatting standingColor = LiteModSynapse.instance.config.getStandingColor(standing);
        return FloatColor.fromChatFormatting(standingColor);
    }

    public void onTick() {
        if (needSynchronizeVoxelmapWaypoints) syncVoxelmapWaypoints();
        final long now = System.currentTimeMillis();
        for (MultiWaypoint point : managedAccountWaypoints.values()) {
            point.onTick(now);
        }
        for (MultiWaypoint point : managedPearlWaypoints.values()) {
            point.onTick(now);
        }
    }

    private void syncVoxelmapWaypoints() {
        try {
            if (!VoxelMapHelper.isVoxelMapActive()) {
                needSynchronizeVoxelmapWaypoints = false;
                return;
            }
            if (VoxelConstants.getVoxelMapInstance().getWaypointManager().getWaypoints().isEmpty()) {
                if (System.currentTimeMillis() > 10000 + LiteModSynapse.instance.getLoginTime()) {
                    // no waypoints loaded after 10s, there probably are no waypoints for the server
                    needSynchronizeVoxelmapWaypoints = false;
                }
                return;
            }

            for (Waypoint vmWaypoint : VoxelMapHelper.getWaypointsByNameImgColor(namePattern, accountImageVm, null)) {
                VoxelMapHelper.deleteWaypoint(vmWaypoint);
            }
            for (Waypoint vmWaypoint : VoxelMapHelper.getWaypointsByNameImgColor(namePattern, pearlImageVm, pearlColor)) {
                final Matcher matcher = namePattern.matcher(vmWaypoint.name);
                if (!matcher.matches()) {
                    System.err.println("Error: pearl waypoint doesn't match pattern: " + vmWaypoint.name);
                    continue;
                }
                final String account = matcher.group("account");
                final MultiWaypoint sPoint = managedPearlWaypoints.computeIfAbsent(account.toLowerCase(), accountLower -> {
                    final Pos pos = new Pos(vmWaypoint.x, vmWaypoint.y, vmWaypoint.z);
                    final MultiWaypoint p = new MultiWaypoint(pos, pearlPrefix, account);
                    p.setImage(pearlImageVm);
                    p.setColor(pearlColor);
                    p.lastPosUpdate = 0;
                    return p;
                });
                if (sPoint.vmWaypoint != null) {
                    VoxelMapHelper.deleteWaypoint(vmWaypoint);
                    continue;
                }
                sPoint.vmWaypoint = vmWaypoint;
                sPoint.updateMapWaypoints();
            }
        } catch (Throwable e) {
            printErrorRateLimited(e);
        }
        needSynchronizeVoxelmapWaypoints = false;
    }

    /**
     * e.g. when jm/vm were enabled/disabled
     */
    public void updateAllWaypoints() {
        for (MultiWaypoint point : managedAccountWaypoints.values()) {
            point.updateMapWaypoints();
        }
        for (MultiWaypoint point : managedPearlWaypoints.values()) {
            point.updateMapWaypoints();
        }
    }

    /**
     * e.g. when mod is disabled
     */
    public void deleteAllWaypoints() {
        for (MultiWaypoint point : managedAccountWaypoints.values()) {
            point.deleteMapWaypoints();
        }
        for (MultiWaypoint point : managedPearlWaypoints.values()) {
            point.deleteMapWaypoints();
        }
    }

    @Override
    public void handlePersonChange(@Nullable Person personOld, @Nullable Person personNew) {
        if (personOld != null) {
            for (String account : personOld.getAccounts()) {
                if (personNew != null && personNew.hasAccount(account) != null) {
                    continue;
                }
                final MultiWaypoint wp = managedAccountWaypoints.get(account.toLowerCase());
                if (wp != null) wp.setColor(getStandingColor(account));
            }
        }
        if (personNew != null) {
            final FloatColor standingColor = getStandingColor(personNew);
            for (String account : personNew.getAccounts()) {
                final MultiWaypoint wp = managedAccountWaypoints.get(account.toLowerCase());
                if (wp != null) wp.setColor(standingColor);
            }
        }
    }

    @Override
    public void handleLargeChange(@NotNull Collection<@NotNull Person> persons) {
        for (Person person : persons) {
            handlePersonChange(null, person);
        }
    }
}
