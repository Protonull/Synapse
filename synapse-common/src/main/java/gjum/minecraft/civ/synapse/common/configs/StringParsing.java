package gjum.minecraft.civ.synapse.common.configs;

import com.google.common.base.Strings;
import org.jetbrains.annotations.NotNull;

public final class StringParsing {
    public static @NotNull String parse(
        String raw,
        final @NotNull String defaultValue
    ) {
        if (raw == null) {
            return defaultValue;
        }
        raw = raw.trim();
        if (raw.isEmpty()) {
            return defaultValue;
        }
        return raw;
    }

    public static int parseInt(
        final String raw,
        final int defaultValue
    ) {
        if (Strings.isNullOrEmpty(raw)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw);
        }
        catch (final NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long parseLong(
        final String raw,
        final long defaultValue
    ) {
        if (Strings.isNullOrEmpty(raw)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(raw);
        }
        catch (final NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean parseBoolean(
        final String raw,
        final boolean defaultValue
    ) {
        if (Strings.isNullOrEmpty(raw)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(raw.trim());
    }
}
