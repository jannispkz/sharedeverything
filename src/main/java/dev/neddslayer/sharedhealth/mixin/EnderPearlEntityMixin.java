package dev.neddslayer.sharedhealth.mixin;

import dev.neddslayer.sharedhealth.SharedHealth;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderPearlEntity.class)
public abstract class EnderPearlEntityMixin extends ThrownItemEntity {

    public EnderPearlEntityMixin(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void onEnderPearlCollision(HitResult hitResult, CallbackInfo ci) {
        if (this.getWorld().isClient()) {
            return;
        }

        ServerWorld world = (ServerWorld) this.getWorld();

        // Check if the gamerule is enabled
        if (!world.getGameRules().getBoolean(SharedHealth.SYNC_ENDER_PEARLS)) {
            return;
        }

        Vec3d landingPos = hitResult.getPos();

        // Get all players from all dimensions
        for (ServerWorld serverWorld : world.getServer().getWorlds()) {
            for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                if (player.isRemoved()) {
                    continue;
                }

                // Reset fall distance to apply consistent damage
                player.setOnGround(false);
                player.fallDistance = 0;

                // If player is in a different dimension, teleport them to this dimension first
                if (player.getServerWorld() != world) {
                    player.teleport(world, landingPos.x, landingPos.y, landingPos.z, java.util.Set.of(), player.getYaw(), player.getPitch(), false);
                } else {
                    // Same dimension, just teleport normally
                    player.requestTeleport(landingPos.x, landingPos.y, landingPos.z);
                }

                // Apply ender pearl damage (5.0 damage)
                player.damage(world, world.getDamageSources().fall(), 5.0f);

                // Play teleport sound
                world.playSound(null, landingPos.x, landingPos.y, landingPos.z,
                    SoundEvents.ENTITY_PLAYER_TELEPORT, SoundCategory.PLAYERS,
                    1.0f, 1.0f);
            }
        }

        // Remove the ender pearl entity
        this.discard();

        // Cancel the original collision handling since we've handled it
        ci.cancel();
    }
}