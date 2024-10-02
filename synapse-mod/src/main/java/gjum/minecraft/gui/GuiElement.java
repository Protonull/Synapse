package gjum.minecraft.gui;

import net.minecraft.client.Minecraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class GuiElement implements GuiParent {
	public static Minecraft mc = Minecraft.getMinecraft();

	@Nullable
	protected GuiParent parent = null;

	@Nonnull
	private Vec2 pos = new Vec2(0, 0);
	@Nonnull
	private Vec2 size = new Vec2(0, 0);
	/**
	 * Share of available flex space the parent should distribute to this element.
	 */
	@Nonnull
	private Vec2 weight = new Vec2(0, 0);
	@Nullable
	private Vec2 minSize;
	@Nullable
	private Vec2 maxSize;
	private boolean dirtyConstraints = true;

	public void setParent(@Nullable GuiParent parent) {
		if (parent != this.parent) {
			invalidateLayout();
			if (parent != null) parent.invalidateLayout();
		}
		this.parent = parent;
	}

	/**
	 * May propagate up (to parent) but never down (to children), to prevent loops.
	 */
	@Override
	public void invalidateLayout() {
		if (parent != null) parent.invalidateLayout();
	}

	public Vec2 getPos() {
		return pos;
	}

	public void setPos(@Nonnull Vec2 pos) {
		this.pos = pos;
	}

	public Vec2 getSize() {
		return size;
	}

	/**
	 * Set this.size to the valid size closest to sizeAvail, within constraints.
	 */
	public void updateSize(Vec2 sizeAvail) {
		if (dirtyConstraints) checkLayout();
		final Vec2 maxSize = getMaxSize();
		final Vec2 minSize = getMinSize();
		size = new Vec2(
				Math.min(maxSize.x, Math.max(minSize.x, sizeAvail.x)),
				Math.min(maxSize.y, Math.max(minSize.y, sizeAvail.y)));
	}

	public GuiElement setFixedSize(Vec2 size) {
		setMaxSize(size);
		setMinSize(size);
		setWeight(new Vec2(0, 0));
		return this;
	}

	public Vec2 getWeight() {
		return weight;
	}

	public GuiElement setWeight(Vec2 weight) {
		this.weight = weight;
		invalidateLayout();
		return this;
	}

	public Vec2 getMaxSize() {
		if (dirtyConstraints) checkLayout();
		return maxSize;
	}

	public GuiElement setMaxSize(Vec2 size) {
		dirtyConstraints = true;
		maxSize = size;
		invalidateLayout();
		return this;
	}

	public Vec2 getMinSize() {
		if (dirtyConstraints) checkLayout();
		return minSize;
	}

	public GuiElement setMinSize(Vec2 size) {
		dirtyConstraints = true;
		minSize = size;
		invalidateLayout();
		return this;
	}

	private void checkLayout() {
		if (minSize == null) minSize = new Vec2(0, 0);
		if (maxSize == null) maxSize = new Vec2(Vec2.LARGE, Vec2.LARGE);

		if (maxSize.x < minSize.x) maxSize = new Vec2(minSize.x, maxSize.y);
		if (maxSize.y < minSize.y) maxSize = new Vec2(maxSize.x, minSize.y);

		dirtyConstraints = false;
	}

	public void draw(Vec2 mouse, Vec2 winSize, float partialTicks) {
	}

	/**
	 * @return true if and only if some element already rendered an overlay.
	 */
	public boolean drawOverlays(Vec2 mouse, Vec2 winSize, float partialTicks) {
		return false;
	}

	/**
	 * @return true if and only if the click changed some element.
	 */
	public boolean onMouseClicked(Vec2 mouse, int mouseButton) {
		return false;
	}

	public void onMouseDragged(Vec2 mouse, Vec2 prevMouse, Vec2 dragStart, int mouseButton, long timeSinceLastClick) {
	}

	public void onMouseReleased(Vec2 mouse, Vec2 dragStart, int state) {
	}

	/**
	 * @return true if and only if the scroll changed some element.
	 */
	public boolean onMouseScrolled(Vec2 mouse, int scrollAmount) {
		return false;
	}

	public void onKeyTyped(char keyChar, int keyCode) {
	}

	/**
	 * Called when the GUI is closed or when this element is removed from the GUI element tree.
	 */
	public void onDestroy() {
	}
}
