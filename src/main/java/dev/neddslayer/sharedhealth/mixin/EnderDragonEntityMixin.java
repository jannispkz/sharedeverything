package dev.neddslayer.sharedhealth.mixin;

import dev.neddslayer.sharedhealth.SharedHealth;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
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
}