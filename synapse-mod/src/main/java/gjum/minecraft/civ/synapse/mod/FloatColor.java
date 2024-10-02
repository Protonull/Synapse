package gjum.minecraft.civ.synapse.mod;

import static gjum.minecraft.civ.synapse.mod.McUtil.getMc;

import java.awt.Color;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.TextFormatting;
import org.jetbrains.annotations.NotNull;

public class FloatColor {
	public float r;
	public float g;
	public float b;

	public FloatColor(float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}

	public int getHex() {
		return 0xff000000
				+ ((int) (r * 255) << 16)
				+ ((int) (g * 255) << 8)
				+ (int) (b * 255);
	}

	public Color toColor() {
		return new Color(r, g, b);
	}

	public static FloatColor fromHex(int hex) {
		return new FloatColor(
				((hex >> 16) & 0xff) / 255.f,
				((hex >> 8) & 0xff) / 255.f,
				(hex & 0xff) / 255.f);
	}

	public static FloatColor fromTextFormatting(@NotNull TextFormatting color) {
		final FontRenderer fontRenderer = getMc().fontRenderer;
		return fromHex(fontRenderer.getColorCode(color.toString().charAt(1)));
	}
}
