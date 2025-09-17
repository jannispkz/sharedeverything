package dev.neddslayer.sharedhealth.mixin;

import dev.neddslayer.sharedhealth.SharedHealth;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {

    @Inject(method = "tryBreakBlock", at = @At("HEAD"), cancellable = true)
    private void sharedhealth$preventLobbyBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (SharedHealth.countdownManager != null && SharedHealth.countdownManager.isInLobbyState()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
