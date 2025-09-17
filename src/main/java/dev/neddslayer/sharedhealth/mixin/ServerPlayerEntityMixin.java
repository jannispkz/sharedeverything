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

    private static boolean deathSummaryShown = false;
    private static long lastDeathTime = 0;

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

        // Check if this is a reset death - if so, ignore it
        if (SharedHealth.isResettingPlayers) {
            return; // This is a countdown reset death, ignore
        }

        // Check if we're in victory celebration - if so, ignore deaths
        if (SharedHealth.dragonDefeatManager != null && SharedHealth.dragonDefeatManager.isVictory()) {
            return; // Victory celebration death, doesn't count as run failure
        }

        // Check if countdown is running - if not, just do normal death
        if (SharedHealth.countdownManager == null || !SharedHealth.countdownManager.isActive()) {
            return; // Normal death, no special effects
        }

        // Check if this death event is part of the same death wave (within 100ms)
        long currentTime = System.currentTimeMillis();
        boolean isNewDeathWave = (currentTime - lastDeathTime) > 100;
        lastDeathTime = currentTime;

        // Reset flag if this is a new death wave
        if (isNewDeathWave) {
            deathSummaryShown = false;
        }

        // Kill all players (use list copy to avoid concurrent modification)
        java.util.List<ServerPlayerEntity> players = new java.util.ArrayList<>(world.getPlayers());
        players.forEach(p -> p.kill(world));

        // Show death title to all players
        net.minecraft.text.Text deathTitle = net.minecraft.text.Text.literal("EVERYONE DIED").formatted(net.minecraft.util.Formatting.RED);

        // Schedule sounds and title to play after respawn (slight delay)
        world.getServer().execute(() -> {
            try {
                Thread.sleep(100); // Small delay to ensure players have respawned
            } catch (InterruptedException e) {
                // Ignore
            }

            for (ServerWorld serverWorld : world.getServer().getWorlds()) {
                for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                    // Send title
                    player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(deathTitle));
                    player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(net.minecraft.text.Text.empty()));
                    player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(10, 100, 20));

                    // Play wither spawn sound directly to player (not at position)
                    player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket(
                        net.minecraft.registry.entry.RegistryEntry.of(net.minecraft.sound.SoundEvents.ENTITY_WITHER_SPAWN),
                        net.minecraft.sound.SoundCategory.MASTER,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        2.0f,
                        1.0f,
                        player.getRandom().nextLong()
                    ));
                }
            }
        });

        // Stop the countdown if it's running
        if (SharedHealth.countdownManager != null) {
            SharedHealth.countdownManager.stop();
        }

        // Send death summary statistics to all players (only once per death wave)
        if (!deathSummaryShown) {
            deathSummaryShown = true;

            for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
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

                    // Calculate total blocks mined by iterating through all blocks
                    int blocksMined = 0;
                    for (net.minecraft.block.Block block : net.minecraft.registry.Registries.BLOCK) {
                        blocksMined += statPlayer.getStatHandler().getStat(net.minecraft.stat.Stats.MINED.getOrCreateStat(block));
                    }

                    // Send compact stats line
                    player.sendMessage(net.minecraft.text.Text.literal("")
                        .append(statPlayer.getName().copy().formatted(net.minecraft.util.Formatting.YELLOW))
                        .append(net.minecraft.text.Text.literal(" - ").formatted(net.minecraft.util.Formatting.DARK_GRAY))
                        .append(net.minecraft.text.Text.literal(totalDistance + "m").formatted(net.minecraft.util.Formatting.WHITE))
                        .append(net.minecraft.text.Text.literal(" | ").formatted(net.minecraft.util.Formatting.DARK_GRAY))
                        .append(net.minecraft.text.Text.literal(blocksMined + " mined").formatted(net.minecraft.util.Formatting.AQUA))
                        .append(net.minecraft.text.Text.literal(" | ").formatted(net.minecraft.util.Formatting.DARK_GRAY))
                        .append(net.minecraft.text.Text.literal(damageTaken + "♥").formatted(net.minecraft.util.Formatting.RED))
                        .append(net.minecraft.text.Text.literal(" | ").formatted(net.minecraft.util.Formatting.DARK_GRAY))
                        .append(net.minecraft.text.Text.literal(mobsKilled + " kills").formatted(net.minecraft.util.Formatting.GREEN)), false);
                }

                player.sendMessage(net.minecraft.text.Text.literal("════════════════════════════════").formatted(net.minecraft.util.Formatting.DARK_GRAY), false);
            }
        }

        // Start shutdown countdown with world deletion
        if (SharedHealth.shutdownManager != null) {
            SharedHealth.shutdownManager.startShutdown();
        }

        // Reset shared components
        SHARED_HEALTH.get(this.getScoreboard()).setHealth(20.0f);
        SHARED_HUNGER.get(this.getScoreboard()).setHunger(20);
        SHARED_SATURATION.get(this.getScoreboard()).setSaturation(20.0f);
    }
}
