package dev.neddslayer.sharedhealth.mixin;

import dev.neddslayer.sharedhealth.SharedHealth;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

@Mixin(LivingEntity.class)
public abstract class StatusEffectMixin {

    private static final Set<RegistryEntry<StatusEffect>> SYNCED_EFFECTS = new HashSet<>();
    private static final ThreadLocal<Boolean> IS_SYNCING = ThreadLocal.withInitial(() -> false);

    static {
        // Positive effects
        SYNCED_EFFECTS.add(StatusEffects.SPEED);
        SYNCED_EFFECTS.add(StatusEffects.JUMP_BOOST);
        SYNCED_EFFECTS.add(StatusEffects.STRENGTH);
        SYNCED_EFFECTS.add(StatusEffects.RESISTANCE);
        SYNCED_EFFECTS.add(StatusEffects.FIRE_RESISTANCE);
        SYNCED_EFFECTS.add(StatusEffects.WATER_BREATHING);
        SYNCED_EFFECTS.add(StatusEffects.INVISIBILITY);
        SYNCED_EFFECTS.add(StatusEffects.NIGHT_VISION);
        SYNCED_EFFECTS.add(StatusEffects.HASTE);
        SYNCED_EFFECTS.add(StatusEffects.LUCK);
        SYNCED_EFFECTS.add(StatusEffects.BAD_LUCK);
        SYNCED_EFFECTS.add(StatusEffects.SLOW_FALLING);
        SYNCED_EFFECTS.add(StatusEffects.CONDUIT_POWER);
        SYNCED_EFFECTS.add(StatusEffects.DOLPHINS_GRACE);
        SYNCED_EFFECTS.add(StatusEffects.HERO_OF_THE_VILLAGE);
        SYNCED_EFFECTS.add(StatusEffects.LEVITATION);
        SYNCED_EFFECTS.add(StatusEffects.RAID_OMEN);

        // Negative (non-damaging) effects
        SYNCED_EFFECTS.add(StatusEffects.SLOWNESS);
        SYNCED_EFFECTS.add(StatusEffects.MINING_FATIGUE);
        SYNCED_EFFECTS.add(StatusEffects.NAUSEA);
        SYNCED_EFFECTS.add(StatusEffects.BLINDNESS);
        SYNCED_EFFECTS.add(StatusEffects.WEAKNESS);
        SYNCED_EFFECTS.add(StatusEffects.BAD_OMEN);
        SYNCED_EFFECTS.add(StatusEffects.DARKNESS);
    }

    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z", at = @At("RETURN"))
    private void onStatusEffectAddedWithSource(StatusEffectInstance effect, net.minecraft.entity.Entity source, CallbackInfoReturnable<Boolean> cir) {
        syncStatusEffectToPlayers(effect, source, cir.getReturnValue());
    }

    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;)Z", at = @At("RETURN"))
    private void onStatusEffectAdded(StatusEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        syncStatusEffectToPlayers(effect, null, cir.getReturnValue());
    }

    private void syncStatusEffectToPlayers(StatusEffectInstance effect, net.minecraft.entity.Entity source, boolean wasAdded) {
        if (!wasAdded || IS_SYNCING.get()) {
            return; // Effect wasn't actually added or we're already syncing (prevent recursion)
        }

        LivingEntity entity = (LivingEntity) (Object) this;

        // Only sync if this is a player
        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }

        ServerWorld world = player.getServerWorld();

        // Check if the gamerule is enabled
        if (!world.getGameRules().getBoolean(SharedHealth.SYNC_STATUS_EFFECTS)) {
            return;
        }

        // Check if this effect should be synced
        if (!SYNCED_EFFECTS.contains(effect.getEffectType())) {
            return;
        }

        // Set syncing flag to prevent recursion
        IS_SYNCING.set(true);
        try {
            // Apply the effect to all other players
            for (ServerWorld serverWorld : world.getServer().getWorlds()) {
                for (ServerPlayerEntity otherPlayer : serverWorld.getPlayers()) {
                    if (otherPlayer != player) {
                        // Check if we need to upgrade an existing effect or add a new one
                        StatusEffectInstance existingEffect = otherPlayer.getStatusEffect(effect.getEffectType());
                        if (existingEffect == null || existingEffect.getAmplifier() < effect.getAmplifier() ||
                            (existingEffect.getAmplifier() == effect.getAmplifier() && existingEffect.getDuration() < effect.getDuration())) {
                            // Create a copy of the effect for the other player
                            StatusEffectInstance copiedEffect = new StatusEffectInstance(
                                effect.getEffectType(),
                                effect.getDuration(),
                                effect.getAmplifier(),
                                effect.isAmbient(),
                                effect.shouldShowParticles(),
                                effect.shouldShowIcon()
                            );
                            // Apply using the public method with source
                            if (source != null) {
                                otherPlayer.addStatusEffect(copiedEffect, source);
                            } else {
                                otherPlayer.addStatusEffect(copiedEffect);
                            }
                        }
                    }
                }
            }
        } finally {
            IS_SYNCING.set(false);
        }
    }

    @Inject(method = "removeStatusEffect", at = @At("RETURN"))
    private void onStatusEffectRemoved(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            return; // Effect wasn't actually removed
        }

        LivingEntity entity = (LivingEntity) (Object) this;

        // Only sync if this is a player
        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }

        ServerWorld world = player.getServerWorld();

        // Check if the gamerule is enabled
        if (!world.getGameRules().getBoolean(SharedHealth.SYNC_STATUS_EFFECTS)) {
            return;
        }

        // Check if this effect should be synced
        if (!SYNCED_EFFECTS.contains(effect)) {
            return;
        }

        // Remove the effect from all other players
        for (ServerWorld serverWorld : world.getServer().getWorlds()) {
            for (ServerPlayerEntity otherPlayer : serverWorld.getPlayers()) {
                if (otherPlayer != player && otherPlayer.hasStatusEffect(effect)) {
                    otherPlayer.removeStatusEffect(effect);
                }
            }
        }
    }
}
