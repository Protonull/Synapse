package gjum.minecraft.civ.synapse.mod.config;

import static gjum.minecraft.civ.synapse.common.Util.scoreSimilarity;
import static gjum.minecraft.civ.synapse.mod.LiteModSynapse.MOD_NAME;

import com.mumfrey.liteloader.util.log.LiteLoaderLogger;
import gjum.minecraft.civ.synapse.common.LinesConfig;
import gjum.minecraft.civ.synapse.mod.LiteModSynapse;
import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.util.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Stores the exact spelling of all account names that were seen on the tab list.
 */
public class AccountsConfig extends LinesConfig {
	protected static final Logger logger = LogManager.getLogger(AccountsConfig.class.getSimpleName());

	private Map<String, String> accounts = new HashMap<>();

	@Override
	protected Collection<String> getLines() {
		return accounts.values();
	}

	@Override
	protected void setLines(Stream<String> newAccounts) {
		accounts = newAccounts.distinct().collect(Collectors.toMap(
				String::toLowerCase, a -> a, (a1, a2) -> a1));
		LiteLoaderLogger.info("[" + MOD_NAME + "] Loaded " + accounts.size() + " accounts");
	}

	@Override
	public void saveNow(@Nullable File file) {
		logger.info("Saving " + this.getClass().getSimpleName() + " to " + file);
		super.saveNow(file);
	}

	/**
	 * @param account Account name, exact case (upper/lower)
	 * @return true if this account was not yet known
	 */
	public boolean addAccount(String account) {
		final boolean wasNew = null == accounts.put(account.toLowerCase(), account);
		if (wasNew) saveLater(null);
		return wasNew;
	}

	@Nonnull
	public List<String> findSimilar(String query, int limit) {
		return findSimilarScoredStream(query)
				.limit(limit)
				.map(Tuple::getFirst)
				.collect(Collectors.toList());
	}

	@Nonnull
	public List<Tuple<String, Float>> findSimilarScored(String query, int limit) {
		return findSimilarScoredStream(query)
				.limit(limit)
				.collect(Collectors.toList());
	}

	@Nonnull
	public Stream<Tuple<String, Float>> findSimilarScoredStream(String query) {
		final String queryLower = query.toLowerCase();
		return streamAccounts()
				.map(a -> new Tuple<>(a, scoreSimilarity(
						queryLower, a.toLowerCase())))
				.sorted(Comparator.comparing(
						Tuple<String, Float>::getSecond
				).reversed()); // highest scores first
	}

	@Nonnull
	public Stream<String> streamAccounts() {
		if (LiteModSynapse.instance.getPersonsRegistry() == null) {
			return accounts.values().stream();
		} else {
			return Stream.concat(
					accounts.values().stream(),
					LiteModSynapse.instance.getPersonsRegistry()
							.getPersons().stream()
							.flatMap(p -> p.getAccounts().stream())
			).distinct();
		}
	}
}