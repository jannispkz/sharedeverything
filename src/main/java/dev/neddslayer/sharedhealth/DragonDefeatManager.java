package dev.neddslayer.sharedhealth;

import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import java.util.Random;

public class DragonDefeatManager {
    private final MinecraftServer server;
    private boolean isVictory;
    private int fireworkTicks;
    private int resetCountdownTicks;
    private boolean statsShown;
    private static final int FIREWORK_DURATION = 200; // 10 seconds * 20 ticks
    private static final int FIREWORK_INTERVAL = 10; // 0.5 seconds * 20 ticks
    private static final int RESET_DELAY = 1200; // 60 seconds * 20 ticks
    private final Random random = new Random();

    public DragonDefeatManager(MinecraftServer server) {
        this.server = server;
        this.isVictory = false;
        this.fireworkTicks = 0;
        this.resetCountdownTicks = 0;
        this.statsShown = false;
    }

    public void onDragonDefeat() {
        if (isVictory) return; // Already celebrating

        this.isVictory = true;
        this.fireworkTicks = FIREWORK_DURATION;
        this.resetCountdownTicks = RESET_DELAY;
        this.statsShown = false;

        // Stop the countdown timer if it's running
        String timeString = "No Timer";
        if (SharedHealth.countdownManager != null && SharedHealth.countdownManager.isActive()) {
            SharedHealth.countdownManager.stop();

            // Calculate elapsed time
            long elapsedMillis = System.currentTimeMillis() - SharedHealth.countdownManager.getStartTime();
            if (elapsedMillis > 0) {
                long totalSeconds = elapsedMillis / 1000;
                long hours = totalSeconds / 3600;
                long minutes = (totalSeconds % 3600) / 60;
                long seconds = totalSeconds % 60;

                if (hours > 0) {
                    timeString = String.format("%d:%02d:%02d", hours, minutes, seconds);
                } else {
                    timeString = String.format("%d:%02d", minutes, seconds);
                }
            }
        }

        // Send victory title to all players
        Text title = Text.literal("DRAGON DEFEATED!").formatted(Formatting.GOLD, Formatting.BOLD);
        Text subtitle = Text.literal("Time: " + timeString).formatted(Formatting.YELLOW);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // Show title for 15 seconds
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(title));
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(subtitle));
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(20, 300, 20)); // 15 seconds = 300 ticks

            // Play victory sound and goat horn
            player.playSoundToPlayer(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.0f, 1.0f);
            player.playSoundToPlayer(net.minecraft.sound.SoundEvent.of(net.minecraft.util.Identifier.of("item.goat_horn.sound.0")), SoundCategory.MASTER, 1.0f, 1.0f);

            // Play Pigstep music disc
            player.playSoundToPlayer(SoundEvents.MUSIC_DISC_PIGSTEP.value(), SoundCategory.RECORDS, 1.0f, 1.0f);

            // Set player to creative mode
            player.changeGameMode(net.minecraft.world.GameMode.CREATIVE);
        }

        // Show statistics in chat
        showStatistics();
    }

    private void showStatistics() {
        if (statsShown) return;
        statsShown = true;

        // Collect statistics
        int totalDeaths = 0;
        int totalMobKills = 0;
        int totalDamageDealt = 0;
        int totalDamageTaken = 0;
        int totalDistanceWalked = 0;
        ServerPlayerEntity mvpPlayer = null;
        int mvpScore = 0;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            totalDeaths += player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.DEATHS));
            totalMobKills += player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.MOB_KILLS));
            totalDamageDealt += player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_DEALT));
            totalDamageTaken += player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_TAKEN));
            totalDistanceWalked += player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.WALK_ONE_CM)) / 100;

            // Calculate MVP based on damage dealt and mob kills
            int playerScore = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_DEALT))
                            + (player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.MOB_KILLS)) * 100);
            if (playerScore > mvpScore) {
                mvpScore = playerScore;
                mvpPlayer = player;
            }
        }

        // Format statistics message
        Text separator = Text.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").formatted(Formatting.DARK_PURPLE, Formatting.BOLD);
        Text header = Text.literal("   🐉 VICTORY STATISTICS 🐉   ").formatted(Formatting.GOLD, Formatting.BOLD);

        // Send statistics to all players
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(separator, false);
            player.sendMessage(header, false);
            player.sendMessage(separator, false);

            player.sendMessage(Text.literal("☠ Total Deaths: ").formatted(Formatting.RED)
                .append(Text.literal(String.valueOf(totalDeaths)).formatted(Formatting.WHITE)), false);

            player.sendMessage(Text.literal("⚔ Mobs Killed: ").formatted(Formatting.GREEN)
                .append(Text.literal(String.valueOf(totalMobKills)).formatted(Formatting.WHITE)), false);

            player.sendMessage(Text.literal("💥 Damage Dealt: ").formatted(Formatting.YELLOW)
                .append(Text.literal(String.format("%.1f", totalDamageDealt / 10.0)).formatted(Formatting.WHITE)), false);

            player.sendMessage(Text.literal("💔 Damage Taken: ").formatted(Formatting.RED)
                .append(Text.literal(String.format("%.1f", totalDamageTaken / 10.0)).formatted(Formatting.WHITE)), false);

            player.sendMessage(Text.literal("👟 Distance Walked: ").formatted(Formatting.AQUA)
                .append(Text.literal(String.format("%dm", totalDistanceWalked)).formatted(Formatting.WHITE)), false);


            if (mvpPlayer != null) {
                player.sendMessage(Text.literal("🏆 MVP: ").formatted(Formatting.GOLD, Formatting.BOLD)
                    .append(Text.literal(mvpPlayer.getName().getString()).formatted(Formatting.YELLOW)), false);
            }

            player.sendMessage(separator, false);
            player.sendMessage(Text.literal("World will reset in 60 seconds!").formatted(Formatting.RED, Formatting.BOLD), false);
            player.sendMessage(separator, false);
        }
    }

    public void tick() {
        if (!isVictory) return;

        // Handle fireworks
        if (fireworkTicks > 0) {
            fireworkTicks--;

            // Spawn fireworks every FIREWORK_INTERVAL ticks
            if (fireworkTicks % FIREWORK_INTERVAL == 0) {
                spawnFireworksForAllPlayers();
            }
        }

        // Handle reset countdown
        if (resetCountdownTicks > 0) {
            resetCountdownTicks--;

            // Show countdown warnings at specific intervals
            int secondsLeft = resetCountdownTicks / 20;
            if (secondsLeft == 30 || secondsLeft == 10 || secondsLeft == 5 ||
                (secondsLeft <= 3 && secondsLeft > 0 && resetCountdownTicks % 20 == 0)) {

                Text warning = Text.literal("World resetting in " + secondsLeft + " seconds!")
                    .formatted(Formatting.RED, Formatting.BOLD);

                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    player.sendMessage(warning, false);
                    player.sendMessage(warning, true); // Also show in action bar

                    // Play warning sound for final countdown
                    if (secondsLeft <= 3) {
                        player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
                            SoundCategory.MASTER, 1.0f, 0.5f);
                    }
                }
            }

            // Trigger world reset (victory mode - rename world)
            if (resetCountdownTicks <= 0) {
                if (SharedHealth.shutdownManager != null) {
                    SharedHealth.shutdownManager.forceImmediateVictoryShutdown();
                }
            }
        }
    }

    private void spawnFireworksForAllPlayers() {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.getWorld() instanceof ServerWorld serverWorld) {
                // Random offset around the player
                double offsetX = (random.nextDouble() - 0.5) * 10;
                double offsetY = random.nextDouble() * 3;
                double offsetZ = (random.nextDouble() - 0.5) * 10;

                Vec3d pos = player.getPos().add(offsetX, offsetY, offsetZ);

                // Create firework item
                ItemStack firework = new ItemStack(Items.FIREWORK_ROCKET);

                // Random flight duration
                int flightDuration = random.nextInt(2) + 1;

                // Create explosion list for component
                java.util.List<net.minecraft.component.type.FireworkExplosionComponent> explosionList = new java.util.ArrayList<>();

                // Random colors
                java.util.List<Integer> colors = new java.util.ArrayList<>();
                for (int i = 0; i < random.nextInt(3) + 1; i++) {
                    colors.add(random.nextInt(0xFFFFFF));
                }

                // Random fade colors
                java.util.List<Integer> fadeColors = new java.util.ArrayList<>();
                if (random.nextBoolean()) {
                    for (int i = 0; i < random.nextInt(2) + 1; i++) {
                        fadeColors.add(random.nextInt(0xFFFFFF));
                    }
                }

                // Random shape
                net.minecraft.component.type.FireworkExplosionComponent.Type shape =
                    net.minecraft.component.type.FireworkExplosionComponent.Type.values()[random.nextInt(5)];

                // Random effects
                boolean hasTrail = random.nextBoolean();
                boolean hasTwinkle = random.nextBoolean();

                // Create explosion component
                // Convert lists to IntList
                it.unimi.dsi.fastutil.ints.IntList colorList = new it.unimi.dsi.fastutil.ints.IntArrayList(colors);
                it.unimi.dsi.fastutil.ints.IntList fadeList = new it.unimi.dsi.fastutil.ints.IntArrayList(fadeColors);

                net.minecraft.component.type.FireworkExplosionComponent explosion =
                    new net.minecraft.component.type.FireworkExplosionComponent(
                        shape,
                        colorList,
                        fadeList,
                        hasTrail,
                        hasTwinkle);

                explosionList.add(explosion);

                // Set firework data using components
                firework.set(net.minecraft.component.DataComponentTypes.FIREWORKS,
                    new net.minecraft.component.type.FireworksComponent(flightDuration, explosionList));

                // Spawn the firework
                FireworkRocketEntity fireworkEntity = new FireworkRocketEntity(
                    serverWorld, pos.x, pos.y, pos.z, firework
                );
                serverWorld.spawnEntity(fireworkEntity);
            }
        }
    }

    public boolean isVictory() {
        return isVictory;
    }

    public void reset() {
        this.isVictory = false;
        this.fireworkTicks = 0;
        this.resetCountdownTicks = 0;
        this.statsShown = false;
    }
}