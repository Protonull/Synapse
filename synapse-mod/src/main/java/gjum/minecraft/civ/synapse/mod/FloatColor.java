package gjum.minecraft.civ.synapse.mod;

import java.awt.Color;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;

public final class FloatColor {
    public float r;
    public float g;
    public float b;

    public FloatColor(
        final float r,
        final float g,
        final float b
    ) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public int getHex() {
        return 0xFF_00_00_00
            + ((int) (this.r * 255) << 16)
            + ((int) (this.g * 255) << 8)
            +  (int) (this.b * 255);
    }

    public @NotNull Color toColor() {
        return new Color(
            this.r,
            this.g,
            this.b
        );
    }

    @Override
    public boolean equals(
        final Object object
    ) {
        if (this == object) {
            return true;
        }
        if (object instanceof final FloatColor other) {
            return this.r == other.r
                && this.g == other.g
                && this.b == other.b;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            getClass(),
            this.r,
            this.g,
            this.b
        );
    }

    @Override
    public String toString() {
        return "%s{r=%f,g=%f,b=%f}".formatted(
            getClass().getSimpleName(),
            this.r,
            this.g,
            this.b
        );
    }

    public static @NotNull FloatColor fromHex(
        final int hex
    ) {
        return new FloatColor(
            ((hex & 0xFF_00_00) >> 16) / 255f,
            ((hex & 0x00_FF_00) >>  8) / 255f,
            ((hex & 0x00_00_FF)      ) / 255f
        );
    }

    public static @NotNull FloatColor fromChatFormatting(
        @NotNull ChatFormatting color
    ) {
        return fromHex(Objects.requireNonNull(
            color.getColor(),
            "Chat format [" + color.getName() + "] is not a valid colour!"
        ));
    }
}
