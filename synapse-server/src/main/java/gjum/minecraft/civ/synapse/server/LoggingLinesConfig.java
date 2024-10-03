package gjum.minecraft.civ.synapse.server;

import gjum.minecraft.civ.synapse.common.LinesConfig;
import java.io.File;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LoggingLinesConfig extends LinesConfig {
    protected static final Logger logger = LoggerFactory.getLogger("Config");

    @Override
    public void saveNow(@Nullable File file) {
        logger.info("Saving " + this.getClass().getSimpleName() + " to " + file);
        super.saveNow(file);
    }
}
