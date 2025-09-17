package dev.neddslayer.sharedhealth.mixin;

import dev.neddslayer.sharedhealth.SharedHealth;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonEntity.class)
public class EnderDragonEntityMixin {

    @Shadow
    private int ticksSinceDeath;

    @Shadow
    private EnderDragonFight fight;

    @Unique
    private boolean hasTriggeredVictory = false;

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void onTickMovement(CallbackInfo ci) {
        EnderDragonEntity dragon = (EnderDragonEntity)(Object)this;

        // Check if dragon just died (ticksSinceDeath starts counting when dragon dies)
        if (!hasTriggeredVictory && ticksSinceDeath > 0 && ticksSinceDeath == 1) {
            if (dragon.getWorld() instanceof ServerWorld && SharedHealth.dragonDefeatManager != null) {
                SharedHealth.dragonDefeatManager.onDragonDefeat();
                hasTriggeredVictory = true;
            }
        }
    }

    // Prevent end portal from generating when dragon dies during a countdown run
    @Inject(method = "updatePostDeath", at = @At("HEAD"), cancellable = true)
    private void preventPortalCreation(CallbackInfo ci) {
        EnderDragonEntity dragon = (EnderDragonEntity)(Object)this;

        // If we're in a victory celebration (countdown was active), skip the normal death behavior
        if (dragon.getWorld() instanceof ServerWorld && SharedHealth.dragonDefeatManager != null
            && SharedHealth.dragonDefeatManager.isVictory()) {

            // Only let the dragon do its death animation but skip portal creation
            if (this.ticksSinceDeath >= 200) {
                // Remove the dragon entity after death animation
                dragon.remove(net.minecraft.entity.Entity.RemovalReason.KILLED);

                // Cancel the rest of updatePostDeath to prevent portal spawn
                ci.cancel();
            }
        }
    }
}