package gjum.minecraft.civ.synapse.mod.config;

import static gjum.minecraft.civ.synapse.common.Util.mapNonNull;

import com.google.gson.annotations.Expose;
import gjum.minecraft.civ.synapse.mod.LiteModSynapse;
import gjum.minecraft.civ.synapse.mod.Person;
import gjum.minecraft.civ.synapse.mod.PersonChangeHandler;
import gjum.minecraft.civ.synapse.mod.PersonsRegistry;
import gjum.minecraft.civ.synapse.mod.Standing;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServerConfig extends JsonConfig {
    /**
     * Whether to enable the mod on this server.
     */
    @Expose
    private boolean enabled = true;

    private static final String defaultCommsAddress = "none!";
    private static final String defaultProxyAddress = "none!";
    @Expose
    private String commsAddress = defaultCommsAddress;
    @Expose
    @NotNull
    private String proxyAddress = defaultProxyAddress;

    /**
     * Decides which factions are important and which ones are secondary/ignored etc.
     * Relevant for rendering (colors) and alerting.
     */
    @Expose
    private HashMap<String, Standing> factionStandings = new HashMap<>();

    @Expose
    @NotNull
    private String defaultFriendlyFaction = "(friendly)";
    @Expose
    @NotNull
    private String defaultHostileFaction = "(hostile)";
    @Expose
    @NotNull
    private String defaultNeutralFaction = "(neutral)";

    private Collection<PersonChangeHandler> changeHandlers = new HashSet<>();

    public ServerConfig() {
        factionStandings.put(defaultFriendlyFaction.toLowerCase(), Standing.FRIENDLY);
        factionStandings.put(defaultHostileFaction.toLowerCase(), Standing.HOSTILE);
        factionStandings.put(defaultNeutralFaction.toLowerCase(), Standing.NEUTRAL);
    }

    @Override
    protected Object getData() {
        return this;
    }

    @Override
    protected void setData(Object data) {
        final ServerConfig other = ((ServerConfig) data);

        enabled = other.enabled;
        commsAddress = other.commsAddress;
        proxyAddress = other.proxyAddress;
        final HashMap<String, Standing> oldFactionStandings = factionStandings;
        factionStandings = other.factionStandings;

        factionStandings.put(defaultFriendlyFaction.toLowerCase(), Standing.FRIENDLY);
        factionStandings.put(defaultHostileFaction.toLowerCase(), Standing.HOSTILE);
        factionStandings.put(defaultNeutralFaction.toLowerCase(), Standing.NEUTRAL);

        final PersonsRegistry personsRegistry = LiteModSynapse.instance.getPersonsRegistry();
        if (personsRegistry != null) {
            final List<Person> personsWithAffectedFactions = personsRegistry.getPersons().stream().filter(person -> {
                for (String faction : person.getFactions()) {
                    if (factionStandings.get(faction.toLowerCase()) != null) return true;
                    if (oldFactionStandings.get(faction.toLowerCase()) != null) return true;
                }
                return false;
            }).collect(Collectors.toList());
            propagateLargeChange(personsWithAffectedFactions);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        saveLater(null);
        LiteModSynapse.instance.checkModActive();
    }

    @NotNull
    public String getProxyAddress() {
        return !proxyAddress.isEmpty() ? proxyAddress : defaultProxyAddress;
    }

    public void setProxyAddress(@NotNull String proxyAddress) {
        this.proxyAddress = proxyAddress;
//        LiteModSynapse.instance.checkCommsAddress();
        saveLater(null);
    }

    public String getCommsAddress() {
        return !commsAddress.isEmpty() ? commsAddress : defaultCommsAddress;
    }

    public void setCommsAddress(String address) {
        commsAddress = address;
//        LiteModSynapse.instance.checkCommsAddress();
        saveLater(null);
    }

    @NotNull
    public String getDefaultFriendlyFaction() {
        return defaultFriendlyFaction;
    }

    @NotNull
    public String getDefaultHostileFaction() {
        return defaultHostileFaction;
    }

    @NotNull
    public String getDefaultNeutralFaction() {
        return defaultNeutralFaction;
    }

    public void setFactionStanding(@NotNull String factionName, @Nullable Standing standing) {
        final String factionLower = factionName.toLowerCase();

        if (standing == null || standing == Standing.UNSET) {
            factionStandings.remove(factionLower);
        } else {
            factionStandings.put(factionLower, standing);
        }
        saveLater(null);

        final PersonsRegistry personsRegistry = LiteModSynapse.instance.getPersonsRegistry();
        if (personsRegistry != null) {
            propagateLargeChange(personsRegistry.personsInFaction(factionLower));
        }
    }

    /**
     * Returns the faction with the most confident {@link Standing}.
     */
    @Nullable
    public String getMostRelevantFaction(@NotNull Collection<String> factions) {
        Standing mostConfidentStanding = null;
        String mostConfidentFaction = null;
        for (String faction : factions) {
            final Standing standing = getFactionStanding(faction);
            if (standing.moreConfidentThan(mostConfidentStanding)) {
                mostConfidentStanding = standing;
                mostConfidentFaction = faction;
            }
        }
        return mostConfidentFaction;
    }

    @NotNull
    public Standing getFactionStanding(@NotNull String faction) {
        return Objects.requireNonNullElse(factionStandings.get(faction.toLowerCase()), Standing.UNSET);
    }

    @NotNull
    public Standing getStanding(@Nullable Person person) {
        if (person == null) return Standing.UNSET;
        final String faction = getMostRelevantFaction(person.getFactions());
        final Standing standing = mapNonNull(faction, this::getFactionStanding);
        return Objects.requireNonNullElse(standing, Standing.UNSET);
    }

    @NotNull
    public Standing getAccountStanding(@NotNull String account) {
        final PersonsRegistry personsRegistry = LiteModSynapse.instance.getPersonsRegistry();
        if (personsRegistry == null) return Standing.UNSET;
        return Objects.requireNonNullElse(mapNonNull(personsRegistry.personByAccountName(account),
                this::getStanding), Standing.UNSET);
    }

    /**
     * Adds/removes factions such that it gets the desired {@link Standing}.
     * If the standing is already as desired, no factions are changed. Otherwise:
     * If FRIENDLY, any HOSTILE factions are removed. Then, if necessary, the default FriendlyFaction is added.
     * If HOSTILE, any FRIENDLY factions are removed. Then, if necessary, the default HostileFaction is added.
     * If NEUTRAL, any FRIENDLY and HOSTILE factions are removed.
     * If UNSET, all factions with a configured Standing are removed.
     */
    public void setPersonStanding(@NotNull Person person, @NotNull Standing standing) {
        if (standing == getStanding(person)) return;

        final Tuple<Collection<String>, String> changes = simulateSetPersonStanding(person, standing);
        final Collection<String> removedFactions = changes.getA();
        final String addedFaction = changes.getB();

        for (String faction : removedFactions) {
            person.removeFaction(faction);
        }

        //noinspection ConstantConditions - we reuse Tuple class but actually allow null here
        if (addedFaction != null) person.addFaction(addedFaction);
    }

    public Tuple<Collection<String>, String> simulateSetPersonStanding(@NotNull Person person, @NotNull Standing standing) {
        Collection<String> removedFactions = new ArrayList<>();
        String addedFaction = null;
        if (standing == getStanding(person)) {
            //noinspection ConstantConditions - we reuse Tuple class but actually allow null here
            return new Tuple<>(removedFactions, addedFaction);
        }
        // TODO since we use the lowercase keys here, faction name upper/lowercase is lost
        switch (standing) {
            case FRIENDLY:
                for (Map.Entry<String, Standing> e : factionStandings.entrySet()) {
                    if (e.getValue() == Standing.HOSTILE) removedFactions.add(e.getKey());
                }
                if (standing != getStanding(person)) addedFaction = getDefaultFriendlyFaction();
                break;
            case HOSTILE:
                for (Map.Entry<String, Standing> e : factionStandings.entrySet()) {
                    if (e.getValue() == Standing.FRIENDLY) removedFactions.add(e.getKey());
                }
                if (standing != getStanding(person)) addedFaction = getDefaultHostileFaction();
                break;
            case NEUTRAL:
                for (Map.Entry<String, Standing> e : factionStandings.entrySet()) {
                    if (e.getValue() == Standing.FRIENDLY || e.getValue() == Standing.HOSTILE) {
                        removedFactions.add(e.getKey());
                    }
                }
                if (standing != getStanding(person)) addedFaction = getDefaultNeutralFaction();
                break;
            case UNSET:
                removedFactions.addAll(factionStandings.keySet());
                break;
        }
        //noinspection ConstantConditions - we reuse Tuple class but actually allow null here
        return new Tuple<>(removedFactions, addedFaction);
    }

    public void registerChangeHandler(@NotNull PersonChangeHandler changeHandler) {
        changeHandlers.add(changeHandler);
    }

    private void propagateLargeChange(Collection<Person> persons) {
        for (PersonChangeHandler changeHandler : changeHandlers) {
            changeHandler.handleLargeChange(persons);
        }
    }
}
