package dev.neddslayer.sharedhealth.mixin;

import com.mojang.authlib.GameProfile;
import dev.neddslayer.sharedhealth.SharedHealth;
import dev.neddslayer.sharedhealth.components.SharedHealthComponent;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dev.neddslayer.sharedhealth.components.SharedComponentsInitializer.*;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {

    @Shadow
    public abstract ServerWorld getServerWorld();

	public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Inject(method = "damage", at = @At("RETURN"))
    public void damageListener(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		// ensure that damage is only taken if the damage listener is handled; you shouldn't be able to punch invulnerable players, etc.
		if (cir.getReturnValue() && this.isAlive()) {
			float currentHealth = this.getHealth();
			SharedHealthComponent component = SHARED_HEALTH.get(this.getScoreboard());
			float knownHealth = component.getHealth();
			if (currentHealth != knownHealth) {
				component.setHealth(currentHealth);
			}

			// Add damage to the feed
			if (SharedHealth.damageFeedManager != null) {
				float actualDamage = knownHealth - currentHealth;
				if (actualDamage > 0) {
					String damageType;

					// Check if damage has an attacker entity (mob or player)
					if (source.getAttacker() != null) {
						// Check if attacker is a player - use their actual name
						if (source.getAttacker() instanceof PlayerEntity) {
							damageType = source.getAttacker().getName().getString();
						} else {
							// For mobs, use the entity type name (e.g., "zombie", "skeleton")
							damageType = source.getAttacker().getType().getName().getString().toLowerCase();
						}
					} else {
						// Fall back to damage type for environmental damage
						damageType = source.getType().msgId().replace("minecraft.", "").replace(".", " ");
					}

					SharedHealth.damageFeedManager.addDamageEntry(this.getName().getString(), actualDamage, damageType);
				}
			}
		}
    }

    @Inject(method = "onDeath", at = @At("TAIL"))
    public void killEveryoneOnDeath(DamageSource damageSource, CallbackInfo ci) {
        ServerWorld world = this.getServerWorld();

        // Kill all players
        world.getPlayers().forEach(p -> p.kill(world));

        // Show death title and play sound to all players
        net.minecraft.text.Text deathTitle = net.minecraft.text.Text.literal("EVERYONE DIED").formatted(net.minecraft.util.Formatting.RED);
        for (ServerWorld serverWorld : world.getServer().getWorlds()) {
            for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                // Send title
                player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(deathTitle));
                player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(net.minecraft.text.Text.empty()));
                player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(10, 100, 20));

                // Play bad sounds (wither spawn and music disc 13)
                player.playSoundToPlayer(net.minecraft.sound.SoundEvents.ENTITY_WITHER_SPAWN, net.minecraft.sound.SoundCategory.MASTER, 2.0f, 1.0f);
                player.playSoundToPlayer(net.minecraft.sound.SoundEvents.MUSIC_DISC_13.value(), net.minecraft.sound.SoundCategory.RECORDS, 0.5f, 1.0f);
            }
        }

        // Stop the countdown if it's running
        if (SharedHealth.countdownManager != null) {
            SharedHealth.countdownManager.stop();
        }

        // Send death summary statistics
        for (ServerWorld serverWorld : world.getServer().getWorlds()) {
            for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                // Send summary header
                player.sendMessage(net.minecraft.text.Text.literal("════════════════════════════════").formatted(net.minecraft.util.Formatting.DARK_GRAY), false);
                player.sendMessage(net.minecraft.text.Text.literal("DEATH SUMMARY").formatted(net.minecraft.util.Formatting.RED, net.minecraft.util.Formatting.BOLD), false);
                player.sendMessage(net.minecraft.text.Text.literal("════════════════════════════════").formatted(net.minecraft.util.Formatting.DARK_GRAY), false);

                // Show stats for each player (compact single line)
                for (ServerPlayerEntity statPlayer : world.getServer().getPlayerManager().getPlayerList()) {
                    // Get statistics
                    int totalDistance = (statPlayer.getStatHandler().getStat(net.minecraft.stat.Stats.CUSTOM.getOrCreateStat(net.minecraft.stat.Stats.WALK_ONE_CM)) +
                                        statPlayer.getStatHandler().getStat(net.minecraft.stat.Stats.CUSTOM.getOrCreateStat(net.minecraft.stat.Stats.SPRINT_ONE_CM))) / 100;
                    int damageTaken = statPlayer.getStatHandler().getStat(net.minecraft.stat.Stats.CUSTOM.getOrCreateStat(net.minecraft.stat.Stats.DAMAGE_TAKEN)) / 10;
                    int mobsKilled = statPlayer.getStatHandler().getStat(net.minecraft.stat.Stats.CUSTOM.getOrCreateStat(net.minecraft.stat.Stats.MOB_KILLS));

                    // Send compact stats line
                    player.sendMessage(net.minecraft.text.Text.literal("")
                        .append(statPlayer.getName().copy().formatted(net.minecraft.util.Formatting.YELLOW))
                        .append(net.minecraft.text.Text.literal(" - ").formatted(net.minecraft.util.Formatting.DARK_GRAY))
                        .append(net.minecraft.text.Text.literal(totalDistance + "m").formatted(net.minecraft.util.Formatting.WHITE))
                        .append(net.minecraft.text.Text.literal(" | ").formatted(net.minecraft.util.Formatting.DARK_GRAY))
                        .append(net.minecraft.text.Text.literal(damageTaken + "♥").formatted(net.minecraft.util.Formatting.RED))
                        .append(net.minecraft.text.Text.literal(" | ").formatted(net.minecraft.util.Formatting.DARK_GRAY))
                        .append(net.minecraft.text.Text.literal(mobsKilled + " kills").formatted(net.minecraft.util.Formatting.GREEN)), false);
                }

                player.sendMessage(net.minecraft.text.Text.literal("════════════════════════════════").formatted(net.minecraft.util.Formatting.DARK_GRAY), false);
            }
        }

        // Start shutdown countdown
        if (SharedHealth.shutdownManager != null) {
            SharedHealth.shutdownManager.startShutdown();
        }

        // Reset shared components
        SHARED_HEALTH.get(this.getScoreboard()).setHealth(20.0f);
        SHARED_HUNGER.get(this.getScoreboard()).setHunger(20);
        SHARED_SATURATION.get(this.getScoreboard()).setSaturation(20.0f);
    }
}
