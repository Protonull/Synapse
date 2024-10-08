package gjum.minecraft.civ.synapse.server.config;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import gjum.minecraft.civ.synapse.common.configs.LinesConfig;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * file format: ACCOUNT\tUUID\n...
 */
public final class UuidsConfig extends LinesConfig {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final BiMap<UUID, String> accounts = HashBiMap.create();

    @Override
    protected @NotNull Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected @NotNull Collection<@NotNull String> getLines() {
        return this.accounts.entrySet()
            .stream()
            .map((account) -> account.getValue() + "\t" + account.getKey())
            .sorted()
            .toList();
    }

    @Override
    protected void setLines(
        final @NotNull Stream<@NotNull String> lines
    ) {
        this.accounts.clear();
        this.accounts.putAll(
            lines.distinct()
                .map((line) -> line.split("\t"))
                .collect(Collectors.toMap(
                    (parts) -> UUID.fromString(parts[1]),
                    (parts) -> parts[0],
                    (oldValue, newValue) -> oldValue
                ))
        );
        LOGGER.info("Loaded {} account uuids", this.accounts.size());
    }

    public @Nullable UUID getUuidForUsername(
        final String account
    ) {
        load(null); // always reload before query; file may have been changed manually
        if (account == null) {
            return null;
        }
        return this.accounts.inverse().get(account);
    }

    public @Nullable String getUsernameByUuid(
        final UUID uuid
    ) {
        return this.accounts.get(uuid);
    }
}
