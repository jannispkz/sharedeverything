package dev.neddslayer.sharedhealth;

import dev.neddslayer.sharedhealth.components.SharedExhaustionComponent;
import dev.neddslayer.sharedhealth.components.SharedHealthComponent;
import dev.neddslayer.sharedhealth.components.SharedHungerComponent;
import dev.neddslayer.sharedhealth.components.SharedSaturationComponent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameRules;

import static dev.neddslayer.sharedhealth.components.SharedComponentsInitializer.*;

public class SharedHealth implements ModInitializer {

    public static final GameRules.Key<GameRules.BooleanRule> SYNC_HEALTH =
            GameRuleRegistry.register("shareHealth", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));
    public static final GameRules.Key<GameRules.BooleanRule> SYNC_HUNGER =
            GameRuleRegistry.register("shareHunger", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));
    public static final GameRules.Key<GameRules.BooleanRule> LIMIT_HEALTH =
            GameRuleRegistry.register("limitHealth", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));
    private static boolean lastHealthValue = true;
    private static boolean lastHungerValue = true;
    public static DamageFeedManager damageFeedManager;

    /**
     * Runs the mod initializer.
     */
    @Override
    public void onInitialize() {
        // Register the /resetscoreboard command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("resetscoreboard")
                .requires(source -> source.hasPermissionLevel(2)) // Requires operator permission
                .executes(context -> {
                    ServerCommandSource source = context.getSource();

                    if (damageFeedManager != null) {
                        damageFeedManager.clearFeed();
                        source.sendFeedback(() -> Text.literal("Damage scoreboard has been reset.").formatted(Formatting.GREEN), true);
                    } else {
                        source.sendError(Text.literal("Damage scoreboard is not initialized."));
                    }

                    return 1;
                }));
        });

        // Initialize damage feed manager when server starts
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            damageFeedManager = new DamageFeedManager(server);
        });

        // Clear damage feed manager when server stops
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (damageFeedManager != null) {
                damageFeedManager.clearFeed();
                damageFeedManager = null;
            }
        });

        ServerTickEvents.END_WORLD_TICK.register((world -> {
            // Update damage feed manager
            if (damageFeedManager != null) {
                damageFeedManager.tick();
            }

            boolean currentHealthValue = world.getGameRules().getBoolean(SYNC_HEALTH);
            boolean currentHungerValue = world.getGameRules().getBoolean(SYNC_HUNGER);
            boolean limitHealthValue = world.getGameRules().getBoolean(LIMIT_HEALTH);
            if (currentHealthValue != lastHealthValue && currentHealthValue) {
                world.getPlayers().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.shareHealth.enabled").formatted(Formatting.GREEN, Formatting.BOLD), false));
                lastHealthValue = true;
            }
            else if (currentHealthValue != lastHealthValue) {
                world.getPlayers().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.shareHealth.disabled").formatted(Formatting.RED, Formatting.BOLD), false));
                lastHealthValue = false;
            }
            if (currentHungerValue != lastHungerValue && currentHungerValue) {
                world.getPlayers().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.shareHunger.enabled").formatted(Formatting.GREEN, Formatting.BOLD), false));
                lastHungerValue = true;
            }
            else if (currentHungerValue != lastHungerValue) {
                world.getPlayers().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.shareHunger.disabled").formatted(Formatting.RED, Formatting.BOLD), false));
                lastHungerValue = false;
            }
            if (world.getGameRules().getBoolean(SYNC_HEALTH)) {
                SharedHealthComponent component = SHARED_HEALTH.get(world.getScoreboard());
                if (component.getHealth() > 20 && limitHealthValue) component.setHealth(20);
                float finalKnownHealth = component.getHealth();
                world.getPlayers().forEach(playerEntity -> {
                    try {
                        float currentHealth = playerEntity.getHealth();

                        if (currentHealth > finalKnownHealth) {
                            playerEntity.damage(world, world.getDamageSources().genericKill(), currentHealth - finalKnownHealth);
                        } else if (currentHealth < finalKnownHealth) {
                            playerEntity.heal(finalKnownHealth - currentHealth);
                        }
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                });
            }
            if (world.getGameRules().getBoolean(SYNC_HUNGER)) {
                SharedHungerComponent component = SHARED_HUNGER.get(world.getScoreboard());
	            SharedSaturationComponent saturationComponent = SHARED_SATURATION.get(world.getScoreboard());
	            SharedExhaustionComponent exhaustionComponent = SHARED_EXHAUSTION.get(world.getScoreboard());
                if (component.getHunger() > 20) component.setHunger(20);
				if (saturationComponent.getSaturation() > 20) saturationComponent.setSaturation(20.0f);
                int finalKnownHunger = component.getHunger();
				float finalKnownSaturation = saturationComponent.getSaturation();
				float finalKnownExhaustion = exhaustionComponent.getExhaustion();
                world.getPlayers().forEach(playerEntity -> {
                    try {
                        float currentHunger = playerEntity.getHungerManager().getFoodLevel();
						float currentSaturation = playerEntity.getHungerManager().getSaturationLevel();
						float currentExhaustion = playerEntity.getHungerManager().exhaustion;

                        if (currentHunger != finalKnownHunger) {
                            playerEntity.getHungerManager().setFoodLevel(finalKnownHunger);
                        }
						if (currentSaturation != finalKnownSaturation) {
							playerEntity.getHungerManager().setSaturationLevel(finalKnownSaturation);
						}
						if (currentExhaustion != finalKnownExhaustion) {
							playerEntity.getHungerManager().exhaustion = finalKnownExhaustion;
						}
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                });
            }
        }));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> handler.player.setHealth(SHARED_HEALTH.get(handler.player.getWorld().getScoreboard()).getHealth()));

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> newPlayer.setHealth(SHARED_HEALTH.get(newPlayer.getWorld().getScoreboard()).getHealth()));
    }
}
