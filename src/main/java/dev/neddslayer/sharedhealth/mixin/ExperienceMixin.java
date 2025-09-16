package dev.neddslayer.sharedhealth.mixin;

import dev.neddslayer.sharedhealth.SharedHealth;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ExperienceMixin {

    private static boolean isSyncing = false;

    @Inject(method = "addExperience", at = @At("TAIL"))
    private void syncExperienceOnAdd(int experience, CallbackInfo ci) {
        syncExperience();
    }

    @Inject(method = "addExperienceLevels", at = @At("TAIL"))
    private void syncExperienceLevelsOnAdd(int levels, CallbackInfo ci) {
        syncExperience();
    }

    @Inject(method = "setExperienceLevel", at = @At("TAIL"))
    private void syncExperienceOnSet(int level, CallbackInfo ci) {
        syncExperience();
    }

    @Inject(method = "setExperiencePoints", at = @At("TAIL"))
    private void syncExperiencePointsOnSet(int points, CallbackInfo ci) {
        syncExperience();
    }

    private void syncExperience() {
        if (isSyncing) {
            return; // Prevent recursion
        }

        ServerPlayerEntity thisPlayer = (ServerPlayerEntity) (Object) this;
        ServerWorld world = thisPlayer.getServerWorld();

        // Check if the gamerule is enabled
        if (!world.getGameRules().getBoolean(SharedHealth.SYNC_EXPERIENCE)) {
            return;
        }

        isSyncing = true;
        try {
            int currentLevel = thisPlayer.experienceLevel;
            int currentTotalXp = thisPlayer.totalExperience;
            float currentProgress = thisPlayer.experienceProgress;

            // Sync to all other players
            for (ServerWorld serverWorld : world.getServer().getWorlds()) {
                for (ServerPlayerEntity otherPlayer : serverWorld.getPlayers()) {
                    if (otherPlayer != thisPlayer) {
                        otherPlayer.experienceLevel = currentLevel;
                        otherPlayer.totalExperience = currentTotalXp;
                        otherPlayer.experienceProgress = currentProgress;

                        // Send experience update packet to client
                        otherPlayer.playerScreenHandler.onContentChanged(otherPlayer.getInventory());
                    }
                }
            }
        } finally {
            isSyncing = false;
        }
    }
}