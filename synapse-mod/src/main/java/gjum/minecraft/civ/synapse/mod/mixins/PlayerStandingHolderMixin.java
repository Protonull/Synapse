package gjum.minecraft.civ.synapse.mod.mixins;

import gjum.minecraft.civ.synapse.mod.Standing;
import java.util.Objects;
import net.minecraft.client.player.AbstractClientPlayer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractClientPlayer.class)
public abstract class PlayerStandingHolderMixin implements Standing.Holder {
    @Unique
    private Standing standing = Standing.UNSET;

    @Override
    public @NotNull Standing synapse$getStanding() {
        return this.standing;
    }

    @Override
    public void synapse$setStanding(
        final @NotNull Standing standing
    ) {
        this.standing = Objects.requireNonNull(standing);
    }
}
