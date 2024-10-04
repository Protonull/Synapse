package gjum.minecraft.civ.synapse.server.config;

import gjum.minecraft.civ.synapse.common.LinesConfig;
import java.io.File;

public abstract class LoggingLinesConfig extends LinesConfig {
    @Override
    public void saveNow(
        final File file
    ) {
        getLogger().info("Saving {} to {}", getClass().getSimpleName(), file);
        super.saveNow(file);
    }
}
