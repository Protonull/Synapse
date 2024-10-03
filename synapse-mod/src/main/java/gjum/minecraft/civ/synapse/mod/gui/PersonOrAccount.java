package gjum.minecraft.civ.synapse.mod.gui;

import gjum.minecraft.civ.synapse.mod.LiteModSynapse;
import gjum.minecraft.civ.synapse.mod.Person;
import gjum.minecraft.civ.synapse.mod.PersonsRegistry;
import gjum.minecraft.civ.synapse.mod.Standing;
import gjum.minecraft.civ.synapse.mod.config.ServerConfig;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class PersonOrAccount {
    // each Nonnull is enforced with check in constructor
    @NotNull
    private final PersonsRegistry personsRegistry = LiteModSynapse.instance.getPersonsRegistry();
    @NotNull
    private final ServerConfig serverConfig = LiteModSynapse.instance.serverConfig;

    // if person is null, account is non-null
    @Nullable
    String account;
    // if account is null, person is non-null
    @Nullable
    Person person;

    public PersonOrAccount(@NotNull Person person) {
        if (personsRegistry == null) throw new IllegalStateException("not on server at the moment");
        this.person = person;
    }

    public PersonOrAccount(@NotNull String account) {
        if (personsRegistry == null) throw new IllegalStateException("not on server at the moment");
        this.account = account;
        this.person = personsRegistry.personByAccountName(account);
    }

    // TODO We use .equals to facilitate observation deduplication. As a result, this class FAILS this assumption: "If two objects are equal according to the equals(Object) method, then calling the hashCode method on each of the two objects must produce the same integer result."
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PersonOrAccount)) return false;
        final PersonOrAccount other = (PersonOrAccount) obj;
        return person == other.person || Objects.equals(account.toLowerCase(), other.account.toLowerCase())
                || (person != null && person.hasAccount(other.account) != null)
                || (other.person != null && other.person.hasAccount(account) != null);
    }

    @Override
    public int hashCode() {
        if (person != null) return person.hashCode() << 1;
        return (account.hashCode() << 1) + 1;
    }

    @NotNull
    public Person personOrCreate() {
        if (person == null) {
            person = personsRegistry.personByAccountNameOrCreate(account);
        }
        return person;
    }

    @NotNull
    public Standing getStanding() {
        if (person == null) return serverConfig.getAccountStanding(account);
        return serverConfig.getStanding(person);
    }

    @NotNull
    public String getName() {
        if (person == null) return account;
        return person.getName();
    }

    @NotNull
    public Collection<String> getAccounts() {
        if (person == null) return Collections.singletonList(account);
        return person.getAccounts();
    }

    @NotNull
    public Collection<String> getFactions() {
        if (person == null) return Collections.emptyList();
        return person.getFactions();
    }
}
