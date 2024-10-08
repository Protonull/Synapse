package gjum.minecraft.civ.synapse.mod.config;

import static gjum.minecraft.civ.synapse.common.Util.scoreSimilarity;

import gjum.minecraft.civ.synapse.common.configs.LinesConfig;
import gjum.minecraft.civ.synapse.mod.LiteModSynapse;
import gjum.minecraft.civ.synapse.mod.PersonsRegistry;
import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores the exact spelling of all account names that were seen on the tab list.
 */
public class AccountsConfig extends LinesConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountsConfig.class);

    private final Map<String, String> accounts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    @Override
    protected @NotNull Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected @NotNull Collection<@NotNull String> getLines() {
        return this.accounts.values();
    }

    @Override
    protected void setLines(
        final @NotNull Stream<@NotNull String> lines
    ) {
        this.accounts.clear();
        this.accounts.putAll(
            lines.distinct()
                .collect(Collectors.toMap(
                    String::toLowerCase,
                    Function.identity(),
                    (oldValue, newValue) -> oldValue
                ))
        );
        LOGGER.info("Loaded {} accounts", this.accounts.size());
    }

    @Override
    public void saveNow(
        final File file
    ) {
        LOGGER.info("Saving {} to {}", getClass().getSimpleName(), file);
        super.saveNow(file);
    }

    /**
     * @param account Account name, exact case (upper/lower)
     * @return true if this account was not yet known
     */
    public boolean addAccount(
        final @NotNull String account
    ) {
        final boolean wasNew = null == this.accounts.put(account.toLowerCase(), account);
        if (wasNew) {
            saveLater(null);
        }
        return wasNew;
    }

    public @NotNull List<@NotNull String> findSimilar(
        final @NotNull String query,
        final int limit
    ) {
        return findSimilarScoredStream(query)
            .limit(limit)
            .map(Tuple::getA)
            .toList();
    }

    public @NotNull List<@NotNull Tuple<@NotNull String, @NotNull Float>> findSimilarScored(
        final @NotNull String query,
        final int limit
    ) {
        return findSimilarScoredStream(query)
            .limit(limit)
            .toList();
    }

    public @NotNull Stream<@NotNull Tuple<@NotNull String, @NotNull Float>> findSimilarScoredStream(
        final @NotNull String query
    ) {
        final String queryLower = query.toLowerCase();
        return streamAccounts()
            .map((username) -> new Tuple<>(username, scoreSimilarity(queryLower, username.toLowerCase())))
            .sorted(Comparator.comparing(Tuple<String, Float>::getB).reversed()); // highest scores first
    }

    public @NotNull Stream<@NotNull String> streamAccounts() {
        final PersonsRegistry registry = LiteModSynapse.instance.getPersonsRegistry();
        if (registry == null) {
            return this.accounts.values().stream();
        }
        return Stream.concat(
            this.accounts.values().stream(),
            registry.getPersons().stream().flatMap((p) -> p.getAccounts().stream())
        ).distinct();
    }
}
