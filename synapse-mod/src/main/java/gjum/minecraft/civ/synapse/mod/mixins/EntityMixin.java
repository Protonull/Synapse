package gjum.minecraft.civ.synapse.mod.mixins;

import gjum.minecraft.civ.synapse.mod.LiteModSynapse;
import gjum.minecraft.civ.synapse.mod.McUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(
        method = "getTeam",
        at = @At("HEAD"),
        cancellable = true
    )
    protected void getTeamHandler(
        final @NotNull CallbackInfoReturnable<PlayerTeam> cir
    ) {
        if (!LiteModSynapse.instance.isModActive()) {
            return;
        }
        if (!((Entity) (Object) this instanceof final Player player)) {
            return;
        }
        final PlayerTeam team = LiteModSynapse.instance.config.getStandingTeam(
            LiteModSynapse.instance.getStanding(McUtil.fullySanitiseComponent(player.getName()))
        );
        if (team != null) {
            cir.setReturnValue(team);
        }
    }

    @Inject(
        method = "getDisplayName",
        at = @At("HEAD"),
        cancellable = true
    )
    protected void getDisplayNameHandler(
        final CallbackInfoReturnable<Component> cir
    ) {
        if (!LiteModSynapse.instance.isModActive()) {
            return;
        }
        if (!((Entity) (Object) this instanceof final Player player)) {
            return;
        }
        final Component displayName = LiteModSynapse.instance.getDisplayNameForAccount(
            McUtil.fullySanitiseComponent(player.getName())
        );
        if (displayName != null) {
            cir.setReturnValue(displayName);
        }
    }
}
