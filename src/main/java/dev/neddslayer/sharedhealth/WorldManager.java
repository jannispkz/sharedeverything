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
                    // Create world (skip difficulty setting as it's causing issues)
                    server.getCommandManager().executeWithPrefix(
                        firstPlayer.getCommandSource().withLevel(4),
                        "mw create " + currentChallengeWorld + " NORMAL"
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
                // Play tick sound
                player.playSoundToPlayer(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(), net.minecraft.sound.SoundCategory.MASTER, 1.0f, 1.0f);
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

        // Teleport each player directly to the world (let multiworld handle spawn)
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            final String playerName = player.getName().getString();
            server.execute(() -> {
                try {
                    // Teleport to world using multiworld command
                    server.getCommandManager().executeWithPrefix(
                        player.getCommandSource(),
                        "mw tp " + currentChallengeWorld
                    );

                    // Wait a bit then teleport to surface at 0,0
                    server.execute(() -> {
                        try {
                            Thread.sleep(500); // Longer delay for world to fully load
                            server.getCommandManager().executeWithPrefix(
                                player.getCommandSource(),
                                "tp 0 100 0"
                            );
                        } catch (Exception e) {
                            System.err.println("[SharedHealth] Error setting surface position for " + playerName + ": " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    System.err.println("[SharedHealth] Error teleporting player " + playerName + ": " + e.getMessage());
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