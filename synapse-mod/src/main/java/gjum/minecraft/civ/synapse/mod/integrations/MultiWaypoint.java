package gjum.minecraft.civ.synapse.mod.integrations;

import static gjum.minecraft.civ.synapse.common.Util.printErrorRateLimited;
import static gjum.minecraft.civ.synapse.mod.McUtil.isJourneyMapLoaded;

import gjum.minecraft.civ.synapse.common.Pos;
import gjum.minecraft.civ.synapse.mod.FloatColor;
import gjum.minecraft.civ.synapse.mod.LiteModSynapse;
import gjum.minecraft.civ.synapse.mod.McUtil;
import gjum.minecraft.civ.synapse.mod.config.GlobalConfig;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import org.jetbrains.annotations.NotNull;

class MultiWaypoint {
    static final Pattern namePattern = Pattern.compile("^(?:Pearl of )?(?<account>[_A-Za-z0-9]{2,16})(?: ?[*+(]\\S+\\)?)? (?<age>now|[0-9]+s|(?:[0-9]+h ?)?[0-9]+min|[0-9]+/[0-9]+ [0-9]+:[0-9]+|old)$");

    String prefix;
    String account;
    @NotNull
    Pos pos;
    FloatColor color;
    String world = "";
    VmImage vmImage = VmImage.small;
    /**
     * whether the location of this waypoint's player hasn't been observed in a long time
     */
    private boolean hiddenForAge = false;
    /**
     * whether this waypoint's player is within entity render distance, making the waypoint redundant
     */
    private boolean hiddenForNearby = false;
    /**
     * whether there is another waypoint in the exact same location (i.e., pearl transporting account)
     */
    private boolean hiddenForOverlap = false;
    long lastPosUpdate = System.currentTimeMillis();

    String suffix = "";

    com.mamiyaotaru.voxelmap.util.Waypoint vmWaypoint;
	journeymap.api.v2.common.waypoint.Waypoint jmWaypoint;

    // periodically update age in waypoint names (fresh ones every 10s, older ones every minute)
    long nextUpdate = 0;

    public MultiWaypoint(@NotNull Pos pos, String prefix, String account) {
        this.pos = pos;
        this.prefix = prefix;
        this.account = account;
    }

    public boolean isVisible() {
        if (hiddenForAge) return false;
        if (hiddenForNearby) return false;
//        if (hiddenForOverlap) return false; // TODO make hiddenForOverlap optional
        return true;
    }

    public String getName() {
        final String accountAndPersonNames = LiteModSynapse.instance.getDisplayNameForAccount(account).getString();
        return prefix + accountAndPersonNames + " y:" + pos.y + suffix;
    }

    public int getDimension() {
        if ("world".equals(world)) return 0; // hot path
        if ("the_end".equals(world.toLowerCase())) return 1;
        if ("nether".equals(world.toLowerCase())) return -1;
        return 0; // custom named worlds (shards) show as overworld
    }

    public MultiWaypoint setPos(@NotNull Pos pos) {
        this.pos = pos;
        lastPosUpdate = System.currentTimeMillis();
        updateMapWaypoints();
        return this;
    }

    public MultiWaypoint setColor(FloatColor color) {
        this.color = color;
        updateMapWaypoints();
        return this;
    }

    public void setHiddenForOverlap(Boolean hidden) {
        hiddenForOverlap = hidden;
        updateMapWaypoints();
    }

    public void setImage(VmImage image) {
        vmImage = Objects.requireNonNullElse(image, VmImage.small);
    }

    private void updateSuffixAndSetUpdateTimer() {
        long now = nextUpdate = System.currentTimeMillis();
        long age = now - lastPosUpdate;
        if (age < 10 * 1000) {
            suffix = " now";
            nextUpdate = lastPosUpdate + 10 * 1000;
        } else if (age < 60 * 1000) {
            suffix = " " + (age / 1000 / 10) * 10 + "s";
            nextUpdate += 10 * 1000;
        } else if (age < 3600 * 1000) {
            suffix = " " + age / 1000 / 60 + "min";
            nextUpdate += 20 * 1000;
        } else if (age < 24 * 3600 * 1000) {
            suffix = " " + age / 3600 / 1000 + "h" + (age / 1000 / 60) % 60 + "min";
            nextUpdate += 20 * 1000;
        } else if (lastPosUpdate == 0) {
            suffix = " old";
            nextUpdate = Long.MAX_VALUE;
        } else {
            suffix = new SimpleDateFormat(" MM/dd HH:mm").format(new Date(lastPosUpdate));
            nextUpdate += 7 * 24 * 3600 * 1000;
        }
        // update shortly after pos update, to deal with dis-/appearing entities
        if (age < 100) nextUpdate = lastPosUpdate + 100;
    }

    public void onTick(long now) {
        // periodically update all waypoints (fresh ones every 10s, older ones every minute)
        if (now > nextUpdate) {
            updateMapWaypoints();
        }
    }

    public void updateMapWaypoints() {
        updateSuffixAndSetUpdateTimer();

        // hide old waypoints
        hiddenForAge = lastPosUpdate < System.currentTimeMillis() - LiteModSynapse.instance.config.getMaxWaypointAge();

        // hide redundant waypoints whose account entity is in radar distance
        final AbstractClientPlayer player = McUtil.findFirstPlayerByName(Minecraft.getInstance().level, account);
        hiddenForNearby = player != null;

        final boolean modActive = LiteModSynapse.instance.isModActive();

        final GlobalConfig globalConfig = LiteModSynapse.instance.config;
        try {
            if (isJourneyMapLoaded()) {
                if (modActive && globalConfig.isUseJourneyMap()) {
                    JourneyMapHelper.updateWaypoint(this);
                } else {
                    JourneyMapHelper.deleteWaypoint(this);
                }
            }
        } catch (Throwable e) {
            printErrorRateLimited(e);
        }

        try {
            if (IntegrationHelpers.isVoxelMapPresent()) {
                if (modActive && globalConfig.isUseVoxelMap()) {
                    VoxelMapHelper.updateWaypoint(this);
                } else {
                    VoxelMapHelper.deleteWaypoint(this);
                }
            }
        } catch (Throwable e) {
            printErrorRateLimited(e);
        }
    }

    public void deleteMapWaypoints() {
        VoxelMapHelper.deleteWaypoint(this);
        JourneyMapHelper.deleteWaypoint(this);
    }

    @Override
    public String toString() {
        return "AccountWaypoint{" +
                "pos=" + pos +
                ", prefix='" + prefix + '\'' +
                ", account='" + account + '\'' +
                ", suffix='" + suffix + '\'' +
                ", color=" + color +
                ", vmImage=" + vmImage +
                ", world='" + world + '\'' +
                ", hiddenForAge=" + hiddenForAge +
                ", hiddenForNearby=" + hiddenForNearby +
                ", hiddenForOverlap=" + hiddenForOverlap +
                ", lastPosUpdate=" + lastPosUpdate +
                ", nextUpdate=" + nextUpdate +
                ", vmWaypoint=" + vmWaypoint +
                ", jmWaypoint=" + jmWaypoint +
                '}';
    }
}
