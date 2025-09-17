package dev.neddslayer.sharedhealth;

import dev.neddslayer.sharedhealth.components.SharedAirComponent;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.border.WorldBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.neddslayer.sharedhealth.components.SharedComponentsInitializer.*;

public class SharedHealth implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("SharedHealth");

    public static final GameRules.Key<GameRules.BooleanRule> SYNC_HEALTH =
            GameRuleRegistry.register("shareHealth", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));
    public static final GameRules.Key<GameRules.BooleanRule> SYNC_HUNGER =
            GameRuleRegistry.register("shareHunger", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));
    public static final GameRules.Key<GameRules.BooleanRule> SYNC_ENDER_PEARLS =
            GameRuleRegistry.register("shareEnderPearls", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));
    public static final GameRules.Key<GameRules.BooleanRule> SYNC_STATUS_EFFECTS =
            GameRuleRegistry.register("shareStatusEffects", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));
    public static final GameRules.Key<GameRules.BooleanRule> SYNC_EXPERIENCE =
            GameRuleRegistry.register("shareExperience", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));
    public static final GameRules.Key<GameRules.BooleanRule> SYNC_AIR =
            GameRuleRegistry.register("shareAir", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));
    public static final GameRules.Key<GameRules.BooleanRule> SYNC_FIRE =
            GameRuleRegistry.register("shareFire", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));
    public static final GameRules.Key<GameRules.BooleanRule> SYNC_FREEZE =
            GameRuleRegistry.register("shareFreeze", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));
    public static final GameRules.Key<GameRules.BooleanRule> LIMIT_HEALTH =
            GameRuleRegistry.register("limitHealth", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));
    private static boolean lastHealthValue = true;
    private static boolean lastHungerValue = true;
    private static boolean lastEnderPearlValue = true;
    private static boolean lastStatusEffectsValue = true;
    private static boolean lastExperienceValue = true;
    private static boolean lastAirValue = true;
    private static boolean lastFireValue = true;
    private static boolean lastFreezeValue = true;
    private static int tabListUpdateCounter = 0;
    private static final double INITIAL_BORDER_DIAMETER = 32.0; // 16 block radius
    public static DamageFeedManager damageFeedManager;
    public static CountdownManager countdownManager;
    public static ShutdownManager shutdownManager;
    public static DragonDefeatManager dragonDefeatManager;
    public static SharedAirManager sharedAirManager;
    public static SharedFireManager sharedFireManager;
    public static SharedFreezeManager sharedFreezeManager;
    public static LeaderboardManager leaderboardManager;
    public static boolean isResettingPlayers = false;
    public static boolean isDeathWave = false;
    public static long deathWaveTime = 0;
    public static java.util.UUID pendingSharedAirPlayer = null;
    public static float pendingSharedAirDamage = -1.0f;
    public static boolean isGlobalDeathProcessing = false;

    public static void setPendingSharedAir(java.util.UUID playerId, float damage) {
        pendingSharedAirPlayer = playerId;
        pendingSharedAirDamage = damage;
    }

    public static void clearPendingSharedAir() {
        pendingSharedAirPlayer = null;
        pendingSharedAirDamage = -1.0f;
    }

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

            // Register the /countdownstart command
            dispatcher.register(CommandManager.literal("countdownstart")
                .requires(source -> true) // Anyone can use this
                .executes(context -> {
                    ServerCommandSource source = context.getSource();

                    if (countdownManager != null) {
                        // Set time to day, clear weather, and enable immediate respawn FIRST
                        for (ServerWorld world : source.getServer().getWorlds()) {
                            world.setTimeOfDay(1000); // Set to day (1000 ticks = morning)
                            world.resetWeather(); // Clear weather
                            world.getGameRules().get(GameRules.DO_IMMEDIATE_RESPAWN).set(true, source.getServer());
                        }

                        // Update shared components to full values
                        ServerPlayerEntity firstPlayer = source.getServer().getPlayerManager().getPlayerList().isEmpty() ? null : source.getServer().getPlayerManager().getPlayerList().get(0);
                        if (firstPlayer != null) {
                            SHARED_HEALTH.get(firstPlayer.getScoreboard()).setHealth(20.0f);
                            SHARED_HUNGER.get(firstPlayer.getScoreboard()).setHunger(20);
                            SHARED_SATURATION.get(firstPlayer.getScoreboard()).setSaturation(20.0f);
                        }

                        // Now start the timer (which will kill players with immediate respawn enabled)
                        countdownManager.start();
                    } else {
                        source.sendError(Text.literal("Countdown manager is not initialized."));
                    }

                    return 1;
                }));

            // Register the /countdownstop command
            dispatcher.register(CommandManager.literal("countdownstop")
                .requires(source -> true) // Anyone can use this
                .executes(context -> {
                    ServerCommandSource source = context.getSource();

                    if (countdownManager != null) {
                        countdownManager.stop();
                        source.sendFeedback(() -> Text.literal("Countdown timer stopped.").formatted(Formatting.RED), true);
                    } else {
                        source.sendError(Text.literal("Countdown manager is not initialized."));
                    }

                    return 1;
                }));

            // Register the /coords command
            dispatcher.register(CommandManager.literal("coords")
                .requires(source -> true) // Everyone can use this
                .then(CommandManager.argument("label", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        String label = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "label");

                        // Get player position
                        if (source.getEntity() instanceof ServerPlayerEntity player) {
                            int x = (int) Math.floor(player.getX());
                            int y = (int) Math.floor(player.getY());
                            int z = (int) Math.floor(player.getZ());
                            String dimension = "";

                            // Add dimension indicator
                            if (player.getWorld().getRegistryKey().getValue().getPath().equals("the_nether")) {
                                dimension = " [Nether]";
                            } else if (player.getWorld().getRegistryKey().getValue().getPath().equals("the_end")) {
                                dimension = " [End]";
                            } else {
                                dimension = " [Overworld]";
                            }

                            // Create the message with blue coordinates
                            Text coordsMessage = Text.literal("")
                                .append(Text.literal(String.format("%d, %d, %d", x, y, z)).formatted(Formatting.BLUE))
                                .append(Text.literal(" [" + label + "]" + dimension).formatted(Formatting.GRAY));

                            // Send to all players
                            for (ServerPlayerEntity recipient : source.getServer().getPlayerManager().getPlayerList()) {
                                recipient.sendMessage(coordsMessage, false);
                            }
                        } else {
                            source.sendError(Text.literal("This command can only be run by players."));
                        }

                        return 1;
                    }))
                .executes(context -> {
                    context.getSource().sendError(Text.literal("Usage: /coords <label>"));
                    return 0;
                }));

            // Register the /reset command
            dispatcher.register(CommandManager.literal("reset")
                .requires(source -> true) // Anyone can use this
                .executes(context -> {
                    ServerCommandSource source = context.getSource();

                    if (shutdownManager != null) {
                        // Show message to all players
                        Text resetMessage = Text.literal("Server resetting immediately!").formatted(Formatting.RED, Formatting.BOLD);
                        for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
                            player.sendMessage(resetMessage, false);
                        }

                        // Immediately trigger world deletion and shutdown
                        shutdownManager.forceImmediateShutdown();
                    } else {
                        source.sendError(Text.literal("Shutdown manager is not initialized."));
                    }

                    return 1;
                }));

            dispatcher.register(CommandManager.literal("leaderboard")
                .requires(source -> true)
                .executes(context -> {
                    if (leaderboardManager != null) {
                        leaderboardManager.sendLeaderboard(context.getSource());
                    } else {
                        context.getSource().sendError(Text.literal("Leaderboard data is not available yet."));
                    }
                    return 1;
                }));
        });

        // Initialize managers when server starts
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            damageFeedManager = new DamageFeedManager(server);
            countdownManager = new CountdownManager(server);
            shutdownManager = new ShutdownManager(server);
            dragonDefeatManager = new DragonDefeatManager(server);
            sharedAirManager = new SharedAirManager(server);
            sharedFireManager = new SharedFireManager(server);
            sharedFreezeManager = new SharedFreezeManager(server);
            leaderboardManager = new LeaderboardManager(server);

            applyInitialWorldBorder(server);
        });

        // Clear managers when server stops
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (damageFeedManager != null) {
                damageFeedManager.clearFeed();
                damageFeedManager = null;
            }
            if (countdownManager != null) {
                countdownManager.stop();
                countdownManager = null;
            }
            if (shutdownManager != null) {
                shutdownManager = null;
            }
            if (dragonDefeatManager != null) {
                dragonDefeatManager.reset();
                dragonDefeatManager = null;
            }
            if (sharedAirManager != null) {
                sharedAirManager = null;
            }
            if (sharedFireManager != null) {
                sharedFireManager.resetState();
                sharedFireManager = null;
            }
            if (sharedFreezeManager != null) {
                sharedFreezeManager.resetState();
                sharedFreezeManager = null;
            }
            if (leaderboardManager != null) {
                leaderboardManager.save();
                leaderboardManager = null;
            }
        });

        ServerTickEvents.END_WORLD_TICK.register((world -> {
            // Only tick managers once per server tick (check if this is the overworld)
            if (world.getRegistryKey() == net.minecraft.world.World.OVERWORLD) {
                // Update damage feed manager
                if (damageFeedManager != null) {
                    damageFeedManager.tick();
                }
                // Update countdown manager
                if (countdownManager != null) {
                    countdownManager.tick();
                }
                // Update shutdown manager
                if (shutdownManager != null) {
                    shutdownManager.tick();
                }
                // Update dragon defeat manager
                if (dragonDefeatManager != null) {
                    dragonDefeatManager.tick();
                }
                if (sharedAirManager != null) {
                    if (world.getGameRules().getBoolean(SYNC_AIR)) {
                        sharedAirManager.tick();
                    } else {
                        SharedAirComponent airComponent = SHARED_AIR.get(world.getScoreboard());
                        airComponent.setAir(airComponent.getMaxAir());
                        airComponent.setDrowningTicks(0);
                        for (ServerWorld serverWorld : world.getServer().getWorlds()) {
                            for (ServerPlayerEntity player : serverWorld.getPlayers()) {
                                player.setAir(player.getMaxAir());
                            }
                        }
                    }
                }
                if (sharedFireManager != null) {
                    sharedFireManager.tick(world.getGameRules().getBoolean(SYNC_FIRE));
                }
                if (sharedFreezeManager != null) {
                    sharedFreezeManager.tick(world.getGameRules().getBoolean(SYNC_FREEZE));
                }
            }

            // Update tab list every 1 second (20 ticks)
            tabListUpdateCounter++;
            if (tabListUpdateCounter >= 20) {
                tabListUpdateCounter = 0;

                // Collect all players from all dimensions
                java.util.List<ServerPlayerEntity> allPlayers = new java.util.ArrayList<>();
                for (ServerWorld serverWorld : world.getServer().getWorlds()) {
                    allPlayers.addAll(serverWorld.getPlayers());
                }

                // Send updates to all players about all players
                for (ServerPlayerEntity player : allPlayers) {
                    for (ServerPlayerEntity viewer : allPlayers) {
                        viewer.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.PlayerListS2CPacket(
                            net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME,
                            player
                        ));
                    }
                }
            }

            boolean currentHealthValue = world.getGameRules().getBoolean(SYNC_HEALTH);
            boolean currentHungerValue = world.getGameRules().getBoolean(SYNC_HUNGER);
            boolean currentEnderPearlValue = world.getGameRules().getBoolean(SYNC_ENDER_PEARLS);
            boolean currentStatusEffectsValue = world.getGameRules().getBoolean(SYNC_STATUS_EFFECTS);
            boolean currentExperienceValue = world.getGameRules().getBoolean(SYNC_EXPERIENCE);
            boolean currentAirValue = world.getGameRules().getBoolean(SYNC_AIR);
            boolean currentFireValue = world.getGameRules().getBoolean(SYNC_FIRE);
            boolean currentFreezeValue = world.getGameRules().getBoolean(SYNC_FREEZE);
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
            if (currentEnderPearlValue != lastEnderPearlValue && currentEnderPearlValue) {
                world.getPlayers().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.shareEnderPearls.enabled").formatted(Formatting.GREEN, Formatting.BOLD), false));
                lastEnderPearlValue = true;
            }
            else if (currentEnderPearlValue != lastEnderPearlValue) {
                world.getPlayers().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.shareEnderPearls.disabled").formatted(Formatting.RED, Formatting.BOLD), false));
                lastEnderPearlValue = false;
            }
            if (currentStatusEffectsValue != lastStatusEffectsValue && currentStatusEffectsValue) {
                world.getPlayers().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.shareStatusEffects.enabled").formatted(Formatting.GREEN, Formatting.BOLD), false));
                lastStatusEffectsValue = true;
            }
            else if (currentStatusEffectsValue != lastStatusEffectsValue) {
                world.getPlayers().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.shareStatusEffects.disabled").formatted(Formatting.RED, Formatting.BOLD), false));
                lastStatusEffectsValue = false;
            }
            if (currentExperienceValue != lastExperienceValue && currentExperienceValue) {
                world.getPlayers().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.shareExperience.enabled").formatted(Formatting.GREEN, Formatting.BOLD), false));
                lastExperienceValue = true;
            }
            else if (currentExperienceValue != lastExperienceValue) {
                world.getPlayers().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.shareExperience.disabled").formatted(Formatting.RED, Formatting.BOLD), false));
                lastExperienceValue = false;
            }
            if (currentAirValue != lastAirValue && currentAirValue) {
                world.getPlayers().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.shareAir.enabled").formatted(Formatting.GREEN, Formatting.BOLD), false));
                SharedAirComponent airComponent = SHARED_AIR.get(world.getScoreboard());
                airComponent.setAir(airComponent.getMaxAir());
                airComponent.setDrowningTicks(0);
                lastAirValue = true;
            }
            else if (currentAirValue != lastAirValue) {
                world.getPlayers().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.shareAir.disabled").formatted(Formatting.RED, Formatting.BOLD), false));
                SharedAirComponent airComponent = SHARED_AIR.get(world.getScoreboard());
                airComponent.setAir(airComponent.getMaxAir());
                airComponent.setDrowningTicks(0);
                lastAirValue = false;
            }
            if (currentFireValue != lastFireValue && currentFireValue) {
                world.getPlayers().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.shareFire.enabled").formatted(Formatting.GREEN, Formatting.BOLD), false));
                lastFireValue = true;
            }
            else if (currentFireValue != lastFireValue) {
                world.getPlayers().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.shareFire.disabled").formatted(Formatting.RED, Formatting.BOLD), false));
                if (sharedFireManager != null) {
                    sharedFireManager.resetState();
                }
                lastFireValue = false;
            }
            if (currentFreezeValue != lastFreezeValue && currentFreezeValue) {
                world.getPlayers().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.shareFreeze.enabled").formatted(Formatting.GREEN, Formatting.BOLD), false));
                lastFreezeValue = true;
            }
            else if (currentFreezeValue != lastFreezeValue) {
                world.getPlayers().forEach(player -> player.sendMessageToClient(Text.translatable("gamerule.shareFreeze.disabled").formatted(Formatting.RED, Formatting.BOLD), false));
                if (sharedFreezeManager != null) {
                    sharedFreezeManager.resetState();
                }
                lastFreezeValue = false;
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

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Set joining player to shared values
            handler.player.setHealth(SHARED_HEALTH.get(handler.player.getWorld().getScoreboard()).getHealth());
            handler.player.getHungerManager().setFoodLevel(SHARED_HUNGER.get(handler.player.getWorld().getScoreboard()).getHunger());
            handler.player.getHungerManager().setSaturationLevel(SHARED_SATURATION.get(handler.player.getWorld().getScoreboard()).getSaturation());
            handler.player.getHungerManager().exhaustion = SHARED_EXHAUSTION.get(handler.player.getWorld().getScoreboard()).getExhaustion();

            SharedAirComponent airComponent = SHARED_AIR.get(handler.player.getServerWorld().getScoreboard());
            if (handler.player.getServerWorld().getGameRules().getBoolean(SYNC_AIR)) {
                int maxAir = handler.player.getMaxAir();
                if (maxAir > airComponent.getMaxAir()) {
                    airComponent.setMaxAir(maxAir);
                    airComponent.setAir(Math.min(airComponent.getAir(), maxAir));
                }
                handler.player.setAir(Math.min(airComponent.getAir(), maxAir));
            } else {
                int maxAir = handler.player.getMaxAir();
                handler.player.setAir(maxAir);
                airComponent.setAir(maxAir);
                airComponent.setMaxAir(maxAir);
                airComponent.setDrowningTicks(0);
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // Always respawn with full health and hunger for clean experience
            newPlayer.setHealth(20.0f);
            newPlayer.getHungerManager().setFoodLevel(20);
            newPlayer.getHungerManager().setSaturationLevel(20.0f);
            newPlayer.getHungerManager().exhaustion = 0.0f;

            // Update shared components to match
            SHARED_HEALTH.get(newPlayer.getWorld().getScoreboard()).setHealth(20.0f);
            SHARED_HUNGER.get(newPlayer.getWorld().getScoreboard()).setHunger(20);
            SHARED_SATURATION.get(newPlayer.getWorld().getScoreboard()).setSaturation(20.0f);
            SHARED_EXHAUSTION.get(newPlayer.getWorld().getScoreboard()).setExhaustion(0.0f);
            SharedAirComponent airComponent = SHARED_AIR.get(newPlayer.getWorld().getScoreboard());
            if (newPlayer.getServerWorld().getGameRules().getBoolean(SYNC_AIR)) {
                int maxAir = newPlayer.getMaxAir();
                if (maxAir > airComponent.getMaxAir()) {
                    airComponent.setMaxAir(maxAir);
                }
                airComponent.setAir(Math.min(airComponent.getAir(), airComponent.getMaxAir()));
                newPlayer.setAir(Math.min(airComponent.getAir(), maxAir));
            } else {
                int maxAir = newPlayer.getMaxAir();
                airComponent.setAir(maxAir);
                airComponent.setMaxAir(maxAir);
                airComponent.setDrowningTicks(0);
                newPlayer.setAir(maxAir);
            }

            // Check if this respawn is part of a death wave (within 2 seconds of death)
            if (isDeathWave && (System.currentTimeMillis() - deathWaveTime) < 2000) {
                // Play death sound and show title after a small delay to ensure client is ready
                newPlayer.getServer().execute(() -> {
                    try {
                        Thread.sleep(100); // Small delay for client to stabilize
                    } catch (InterruptedException e) {
                        // Ignore
                    }

                    // Send death title
                    Text deathTitle = Text.literal("EVERYONE DIED").formatted(Formatting.RED);
                    newPlayer.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(deathTitle));
                    newPlayer.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(Text.empty()));
                    newPlayer.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(10, 100, 20));

                    // Play wither spawn sound with higher volume to override other sounds
                    newPlayer.playSoundToPlayer(
                        SoundEvents.ENTITY_WITHER_SPAWN,
                        SoundCategory.MASTER,
                        3.0f,  // Higher volume to override other sounds
                        1.0f
                    );
                });

                // Clear the death wave flag after 2 seconds
                if ((System.currentTimeMillis() - deathWaveTime) > 1500) {
                    isDeathWave = false;
                }
            }
        });
    }

    private static void applyInitialWorldBorder(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            WorldBorder border = world.getWorldBorder();
            BlockPos spawn = world.getSpawnPos();
            border.setCenter(spawn.getX() + 0.5, spawn.getZ() + 0.5);
            border.setSize(INITIAL_BORDER_DIAMETER);
            world.getGameRules().get(GameRules.DO_IMMEDIATE_RESPAWN).set(true, server);
        }
    }

    public static String computeMvpName(MinecraftServer server) {
        ServerPlayerEntity bestPlayer = null;
        int bestScore = Integer.MIN_VALUE;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            int score = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_DEALT))
                + player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.MOB_KILLS)) * 100;
            if (score > bestScore) {
                bestScore = score;
                bestPlayer = player;
            }
        }
        return bestPlayer != null ? bestPlayer.getName().getString() : null;
    }
}
