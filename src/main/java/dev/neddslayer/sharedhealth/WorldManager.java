package dev.neddslayer.sharedhealth;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class WorldManager {
    private final MinecraftServer server;
    private boolean isCountingDown;
    private int preCountdownTicks;
    private String currentChallengeWorld;
    private static final int PRE_COUNTDOWN_DELAY = 100; // 5 seconds * 20 ticks

    public WorldManager(MinecraftServer server) {
        this.server = server;
        this.isCountingDown = false;
        this.preCountdownTicks = 0;
        this.currentChallengeWorld = null;
    }

    public void startWorldCreation() {
        this.isCountingDown = true;
        this.preCountdownTicks = PRE_COUNTDOWN_DELAY;

        // Create a unique world name with timestamp
        this.currentChallengeWorld = "challenge_" + System.currentTimeMillis();

        // Create the new world using commands (executed by first available player)
        ServerPlayerEntity firstPlayer = server.getPlayerManager().getPlayerList().isEmpty() ? null : server.getPlayerManager().getPlayerList().get(0);
        if (firstPlayer != null) {
            server.execute(() -> {
                try {
                    // Create world
                    server.getCommandManager().executeWithPrefix(
                        firstPlayer.getCommandSource().withLevel(4),
                        "mw create " + currentChallengeWorld + " NORMAL"
                    );

                    // Set difficulty to hard
                    server.getCommandManager().executeWithPrefix(
                        firstPlayer.getCommandSource().withLevel(4),
                        "mw difficulty HARD"
                    );

                    System.out.println("[SharedHealth] Created challenge world: " + currentChallengeWorld);
                } catch (Exception e) {
                    System.err.println("[SharedHealth] Error creating world: " + e.getMessage());
                }
            });
        }
    }

    public void tick() {
        if (!isCountingDown) {
            return;
        }

        preCountdownTicks--;

        // Calculate seconds remaining
        int secondsLeft = (preCountdownTicks + 19) / 20; // Round up

        // Show countdown every second
        if (preCountdownTicks % 20 == 0 && secondsLeft > 0) {
            Text countdownMessage = Text.literal("Teleporting in: " + secondsLeft).formatted(Formatting.GOLD, Formatting.BOLD);
            server.getPlayerManager().getPlayerList().forEach(player -> {
                player.sendMessage(countdownMessage, true);
            });
        }

        // Teleport everyone when countdown reaches 0
        if (preCountdownTicks <= 0) {
            isCountingDown = false;
            teleportAllPlayers();
        }
    }

    private void teleportAllPlayers() {
        if (currentChallengeWorld == null) {
            return;
        }

        // First teleport players to the world, then fix their position to surface
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            server.execute(() -> {
                try {
                    // Teleport to world first
                    server.getCommandManager().executeWithPrefix(
                        player.getCommandSource(),
                        "mw tp " + currentChallengeWorld
                    );

                    // Then teleport to surface at 0,0 to avoid caves
                    Thread.sleep(100); // Small delay to ensure world load
                    server.getCommandManager().executeWithPrefix(
                        player.getCommandSource(),
                        "tp 0 ~ 0"
                    );
                } catch (Exception e) {
                    System.err.println("[SharedHealth] Error teleporting player " + player.getName().getString() + ": " + e.getMessage());
                }
            });
        }

        // Start the actual countdown timer now that players are in the new world
        if (SharedHealth.countdownManager != null) {
            SharedHealth.countdownManager.start();
        }
    }

    public void deleteCurrentWorld() {
        if (currentChallengeWorld == null) {
            return;
        }

        // Teleport everyone back to overworld first using multiworld commands
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            server.execute(() -> {
                try {
                    server.getCommandManager().executeWithPrefix(
                        player.getCommandSource(),
                        "mw tp minecraft:overworld"
                    );
                } catch (Exception e) {
                    System.err.println("[SharedHealth] Error teleporting player back: " + e.getMessage());
                }
            });
        }

        // Since delete is console-only, we'll just leave the world for now
        // The multiworld mod will handle cleanup, and the world name is unique each time
        System.out.println("[SharedHealth] Players returned to overworld. Challenge world " + currentChallengeWorld + " left for manual cleanup.");

        currentChallengeWorld = null;
    }

    public boolean isCountingDown() {
        return isCountingDown;
    }

    public String getCurrentChallengeWorld() {
        return currentChallengeWorld;
    }
}