package dev.neddslayer.sharedhealth.mixin;

import dev.neddslayer.sharedhealth.SharedHealth;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MilkBucketItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MilkBucketItem.class)
public abstract class MilkBucketItemMixin {

    @Inject(method = "finishUsing", at = @At("TAIL"))
    private void sharedhealth$clearStatusEffectsForAll(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient) {
            return;
        }

        if (!(user instanceof ServerPlayerEntity player)) {
            return;
        }

        ServerWorld serverWorld = player.getServerWorld();
        MinecraftServer server = serverWorld.getServer();

        if (!serverWorld.getGameRules().getBoolean(SharedHealth.SYNC_STATUS_EFFECTS)) {
            return;
        }

        for (ServerWorld otherWorld : server.getWorlds()) {
            for (ServerPlayerEntity otherPlayer : otherWorld.getPlayers()) {
                // clear all status effects, matching vanilla milk behaviour across the team
                otherPlayer.clearStatusEffects();
            }
        }
    }
}
