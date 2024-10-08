package gjum.minecraft.civ.synapse.common.configs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public abstract class LinesConfig {
    private static final long SAVE_LATER_TIMEOUT = 300;
    private long lastSaveTime = 0;
    private final ScheduledExecutorService delayedSaveServicePool = Executors.newSingleThreadScheduledExecutor();

    private volatile File configFile;
    protected boolean isLoading = false;

    protected abstract @NotNull Logger getLogger();

    protected abstract @NotNull Collection<@NotNull String> getLines();

    protected abstract void setLines(
        @NotNull Stream<@NotNull String> lines
    );

    public void load(
        final File file
    ) {
        synchronized (this) {
            if (file != null) {
                this.configFile = file;
            }
            final FileReader fileReader;
            try {
                fileReader = new FileReader(this.configFile);
            }
            catch (final FileNotFoundException e) {
                return;
            }
            this.isLoading = true;
            try (fileReader; final var bufferedReader = new BufferedReader(fileReader)) {
                setLines(bufferedReader.lines());
            }
            catch (final IOException e) {
                getLogger().warn("Could not load [{}] from [{}]", getClass().getSimpleName(), this.configFile.getPath());
            }
            finally {
                this.isLoading = false;
            }
            getLogger().info("Loaded [{}] from [{}]", getClass().getSimpleName(), this.configFile.getPath());
        }
    }

    public void saveLater(
        final File file
    ) {
        final long requestTimestamp = System.currentTimeMillis();
        this.delayedSaveServicePool.schedule(
            () -> {
                if (this.lastSaveTime > requestTimestamp) {
                    // saveNow() was called between this saveLater() call and here, do nothing
                    return;
                }
                saveNow(file);
            },
            SAVE_LATER_TIMEOUT,
            TimeUnit.MILLISECONDS
        );
    }

    public void saveNow(
        final File file
    ) {
        synchronized (this) {
            if (this.isLoading) {
                throw new ConcurrentModificationException("Cannot save while loading");
            }
            if (file != null) {
                this.configFile = file;
            }
            this.lastSaveTime = System.currentTimeMillis();
            this.configFile.getParentFile().mkdirs();
            try (final var out = new FileOutputStream(this.configFile)) {
                out.write(String.join("\n", getLines()).getBytes(StandardCharsets.UTF_8));
            }
            catch (final IOException e) {
                getLogger().warn("Could not save [{}] to [{}]", getClass().getSimpleName(), this.configFile.getPath());
                return;
            }
            getLogger().info("Saved [{}] to [{}]", getClass().getSimpleName(), this.configFile.getPath());
        }
    }
}
