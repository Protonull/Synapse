package gjum.minecraft.civ.synapse.mod.config;

import static gjum.minecraft.civ.synapse.common.Util.sortedUniqListIgnoreCase;
import static gjum.minecraft.civ.synapse.mod.LiteModSynapse.MOD_NAME;

import gjum.minecraft.civ.synapse.mod.Person;
import gjum.minecraft.civ.synapse.mod.PersonChangeHandler;
import gjum.minecraft.civ.synapse.mod.PersonsRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PersonsConfig extends JsonConfig implements PersonChangeHandler {
    private PersonsRegistry personsRegistry = new PersonsRegistry();

    public PersonsConfig() {
        personsRegistry.registerChangeHandler(this);
    }

    public PersonsRegistry getPersonsRegistry() {
        return personsRegistry;
    }

    @Override
    protected Object getData() {
        Map<String, Map<String, Collection<String>>> data = new HashMap<>();
        for (Person person : personsRegistry.getPersons()) {
            final Map<String, Collection<String>> personData = data.computeIfAbsent(person.getName(), p -> new HashMap<>());
            personData.put("accounts", sortedUniqListIgnoreCase(person.getAccounts()));
            personData.put("factions", sortedUniqListIgnoreCase(person.getFactions()));
        }
        return data;
    }

    @Override
    protected void setData(Object data) {
        final Set<Map.Entry<String, Map<String, Collection<String>>>>
                entries = ((Map<String, Map<String, Collection<String>>>) data).entrySet();
        Collection<Person> persons = new ArrayList<>(entries.size());
        for (Map.Entry<String, Map<String, Collection<String>>> entry : entries) {
            final String personName = entry.getKey();
            final Map<String, Collection<String>> personIn = entry.getValue();
            HashSet<String> factions = new HashSet<>();
            if (personIn.get("factions") != null) {
                factions.addAll(personIn.get("factions"));
            }
            HashSet<String> accounts = new HashSet<>();
            if (personIn.get("accounts") != null) {
                accounts.addAll(personIn.get("accounts"));
            }
            persons.add(new Person(getPersonsRegistry(), personName, factions, accounts, null));
        }
        personsRegistry.loadPersons(persons);
        logger.info("[" + MOD_NAME + "] Loaded {} persons", persons.size());
    }

    @Override
    public void handlePersonChange(@Nullable Person personOld, @Nullable Person personNew) {
        saveLater(null);
    }

    @Override
    public void handleLargeChange(@NotNull Collection<@NotNull Person> persons) {
        saveLater(null);
    }
}
