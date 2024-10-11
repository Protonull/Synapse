package gjum.minecraft.civ.synapse.server.config;

import com.google.common.collect.Collections2;
import gjum.minecraft.civ.synapse.common.configs.LinesConfig;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * file format: ACCOUNT\tUUID\n... or ACCOUNT\n...
 */
public final class AccountsListConfig extends LinesConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountsListConfig.class);

    private final UuidsConfig uuidMapper;
    private final Set<UUID> uuids;

    public AccountsListConfig(
        final @NotNull UuidsConfig uuidMapper
    ) {
        this.uuidMapper = Objects.requireNonNull(uuidMapper);
        this.uuids = new HashSet<>();
    }

    @Override
    protected @NotNull Logger getLogger() {
        return LOGGER;
    }

    @Override
    protected @NotNull Collection<@NotNull String> getLines() {
        return this.uuids.stream()
            .map((uuid) -> this.uuidMapper.getUsernameByUuid(uuid) + "\t" + uuid)
            .sorted()
            .toList();
    }

    @Override
    protected void setLines(
        final @NotNull Stream<@NotNull String> lines
    ) {
        this.uuids.clear();
        this.uuids.addAll(
            lines
                .map((line) -> {
                    final String[] parts = line.split("\t");
                    if (parts.length < 2 || parts[1].trim().isEmpty()) {
                        return this.uuidMapper.getUuidForUsername(parts[0].trim());
                    }
                    return UUID.fromString(parts[1].trim());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
        );
        LOGGER.info("Loaded list of {} accounts by playerUuid", this.uuids.size());
    }

    public void setList(
        final @NotNull Collection<String> accounts
    ) {
        this.uuids.clear();
        this.uuids.addAll(Collections2.transform(accounts, this.uuidMapper::getUuidForUsername));
        this.uuids.removeIf(Objects::isNull);
        saveLater(null);
    }

    public boolean contains(
        final UUID uuid
    ) {
        return uuid != null && this.uuids.contains(uuid);
    }
}
