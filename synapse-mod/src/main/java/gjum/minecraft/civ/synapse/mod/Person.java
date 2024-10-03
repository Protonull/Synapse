package gjum.minecraft.civ.synapse.mod;

import gjum.minecraft.civ.synapse.common.Util;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Person implements Cloneable {
    private final PersonsRegistry registry;
    private String name;
    private Collection<String> factions;
    private Collection<String> accounts;
    private String notes;

    public Person(
        final @NotNull PersonsRegistry registry,
        final @NotNull String name,
        final @NotNull Collection<@NotNull String> factions,
        final @NotNull Collection<@NotNull String> accounts,
        final String notes
    ) {
        this.registry = Objects.requireNonNull(registry);
        this.name = Objects.requireNonNull(name);
        this.factions = Util.sortedUniqListIgnoreCase(Objects.requireNonNull(factions));
        this.accounts = Util.sortedUniqListIgnoreCase(Objects.requireNonNull(accounts));
        this.notes = notes;
    }

    private Person cloneMe() {
        try {
            return (Person) clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public String getName() {
        return name;
    }

    public boolean setName(String name) {
        final Person oldPerson = cloneMe();
        this.name = name;
        boolean accepted = registry.propagatePersonChange(oldPerson, this);
        if (!accepted) this.name = oldPerson.name;
        return accepted;
    }

    public boolean isMain(String account) {
        if (name.toLowerCase().equals(account.toLowerCase())) return true;
        final String personClean = name.toLowerCase().replaceAll("_+", "");
        final String accountClean = account.toLowerCase().replaceAll("_+", "");
        return accountClean.startsWith(personClean) || accountClean.endsWith(personClean)
                || personClean.startsWith(accountClean) || personClean.endsWith(accountClean);
    }

    @NotNull
    public Collection<String> getAccounts() {
        return accounts;
    }

    @Nullable
    public String hasAccount(@Nullable String account) {
        return Util.containsIgnoreCase(account, accounts);
    }

    public void addAccount(String account) {
        final Person oldPerson = cloneMe();
        oldPerson.accounts = new ArrayList<>(accounts);
        accounts.add(account);
        accounts = Util.sortedUniqListIgnoreCase(accounts);
        registry.propagatePersonChange(oldPerson, this);
    }

    public void removeAccount(String account) {
        account = hasAccount(account);
        if (account == null) return;
        final Person oldPerson = cloneMe();
        oldPerson.accounts = new ArrayList<>(accounts);
        accounts.remove(account);
        registry.propagatePersonChange(oldPerson, this);
    }

    public void setAccounts(Collection<String> newAccounts) {
        final Person oldPerson = cloneMe();
        oldPerson.accounts = new ArrayList<>(this.accounts);
        this.accounts = Util.sortedUniqListIgnoreCase(newAccounts);
        registry.propagatePersonChange(oldPerson, this);
    }

    @NotNull
    public Collection<String> getFactions() {
        return new ArrayList<>(factions);
    }

    @Nullable
    public String hasFaction(@Nullable String faction) {
        return Util.containsIgnoreCase(faction, factions);
    }

    public void addFaction(String faction) {
        final Person oldPerson = cloneMe();
        oldPerson.factions = new ArrayList<>(factions);
        factions.add(faction);
        factions = Util.sortedUniqListIgnoreCase(factions);
        registry.propagatePersonChange(oldPerson, this);
    }

    public void removeFaction(String faction) {
        faction = hasFaction(faction);
        if (faction == null) return;
        final Person oldPerson = cloneMe();
        oldPerson.factions = new ArrayList<>(factions);
        factions.remove(faction);
        registry.propagatePersonChange(oldPerson, this);
    }

    public void setFactions(Collection<String> newFactions) {
        final Person oldPerson = cloneMe();
        oldPerson.factions = new ArrayList<>(this.factions);
        this.factions = Util.sortedUniqListIgnoreCase(newFactions);
        registry.propagatePersonChange(oldPerson, this);
    }

    @NotNull
    public String getNotes() {
        if (notes == null) return "";
        return notes;
    }

    public void setNotes(@Nullable String notes) {
        final Person oldPerson = cloneMe();
        this.notes = notes;
        registry.propagatePersonChange(oldPerson, this);
    }
}
