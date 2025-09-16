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
    private boolean isCreatingDimensions;
    private int preCountdownTicks;
    private int dimensionCreationStage;
    private int dimensionCreationTicks;
    private String currentChallengeWorld;
    private static final int PRE_COUNTDOWN_DELAY = 100; // 5 seconds * 20 ticks
    private static final int DIMENSION_DELAY = 40; // 2 seconds * 20 ticks

    public WorldManager(MinecraftServer server) {
        this.server = server;
        this.isCountingDown = false;
        this.isCreatingDimensions = false;
        this.preCountdownTicks = 0;
        this.dimensionCreationStage = 0;
        this.dimensionCreationTicks = 0;
        this.currentChallengeWorld = null;
    }

    public void startWorldCreation() {
        // Create a unique world name with timestamp
        this.currentChallengeWorld = "challenge_" + System.currentTimeMillis();

        // Start dimension creation process
        this.isCreatingDimensions = true;
        this.dimensionCreationStage = 0;
        this.dimensionCreationTicks = 0;
    }

    public void tick() {
        // Handle dimension creation first
        if (isCreatingDimensions) {
            dimensionCreationTicks++;

            // Show progress based on stage
            String[] stages = {"Creating Overworld...", "Creating Nether...", "Creating End..."};
            if (dimensionCreationStage < stages.length) {
                Text progressMessage = Text.literal(stages[dimensionCreationStage]).formatted(Formatting.AQUA, Formatting.BOLD);
                server.getPlayerManager().getPlayerList().forEach(player -> {
                    player.sendMessage(progressMessage, true);
                });

                // Create dimension after showing message for a moment
                if (dimensionCreationTicks == 20) { // 1 second after showing message
                    createDimension(dimensionCreationStage);
                }

                // Move to next stage after delay
                if (dimensionCreationTicks >= DIMENSION_DELAY) {
                    dimensionCreationStage++;
                    dimensionCreationTicks = 0;

                    // If all dimensions created, start teleport countdown
                    if (dimensionCreationStage >= stages.length) {
                        isCreatingDimensions = false;
                        isCountingDown = true;
                        preCountdownTicks = PRE_COUNTDOWN_DELAY;
                    }
                }
            }
            return;
        }

        // Handle teleport countdown
        if (isCountingDown) {
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
    }

    private void createDimension(int stage) {
        ServerPlayerEntity firstPlayer = server.getPlayerManager().getPlayerList().isEmpty() ? null : server.getPlayerManager().getPlayerList().get(0);
        if (firstPlayer != null) {
            server.execute(() -> {
                try {
                    switch (stage) {
                        case 0: // Overworld
                            server.getCommandManager().executeWithPrefix(
                                firstPlayer.getCommandSource().withLevel(4),
                                "mw create " + currentChallengeWorld + " NORMAL"
                            );
                            break;
                        case 1: // Nether
                            server.getCommandManager().executeWithPrefix(
                                firstPlayer.getCommandSource().withLevel(4),
                                "mw create " + currentChallengeWorld + "_nether NETHER"
                            );
                            break;
                        case 2: // End
                            server.getCommandManager().executeWithPrefix(
                                firstPlayer.getCommandSource().withLevel(4),
                                "mw create " + currentChallengeWorld + "_the_end END"
                            );
                            break;
                    }
                } catch (Exception e) {
                    System.err.println("[SharedHealth] Error creating dimension " + stage + ": " + e.getMessage());
                }
            });
        }
    }

    private void teleportAllPlayers() {
        if (currentChallengeWorld == null) {
            return;
        }

        // Disable fall damage once for all players
        ServerPlayerEntity firstPlayer = server.getPlayerManager().getPlayerList().isEmpty() ? null : server.getPlayerManager().getPlayerList().get(0);
        if (firstPlayer != null) {
            server.execute(() -> {
                try {
                    server.getCommandManager().executeWithPrefix(
                        firstPlayer.getCommandSource(),
                        "gamerule fallDamage false"
                    );
                } catch (Exception e) {
                    System.err.println("[SharedHealth] Error disabling fall damage: " + e.getMessage());
                }
            });
        }

        // Teleport each player to the world
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            final String playerName = player.getName().getString();
            server.execute(() -> {
                try {
                    // Teleport to world using multiworld command
                    server.getCommandManager().executeWithPrefix(
                        player.getCommandSource(),
                        "mw tp " + currentChallengeWorld
                    );

                    // Wait a bit then teleport to height 500
                    server.execute(() -> {
                        try {
                            Thread.sleep(500); // Delay for world to fully load
                            server.getCommandManager().executeWithPrefix(
                                player.getCommandSource(),
                                "tp 0 500 0"
                            );
                        } catch (Exception e) {
                            System.err.println("[SharedHealth] Error setting position for " + playerName + ": " + e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    System.err.println("[SharedHealth] Error teleporting player " + playerName + ": " + e.getMessage());
                }
            });
        }

        // Re-enable fall damage after 25 seconds
        if (firstPlayer != null) {
            server.execute(() -> {
                try {
                    Thread.sleep(25000); // 25 seconds
                    server.getCommandManager().executeWithPrefix(
                        firstPlayer.getCommandSource(),
                        "gamerule fallDamage true"
                    );
                } catch (Exception e) {
                    System.err.println("[SharedHealth] Error re-enabling fall damage: " + e.getMessage());
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

        // Since delete is console-only, we'll leave the worlds for manual cleanup
        // The worlds have unique timestamp names so they won't conflict
        System.out.println("[SharedHealth] Players returned to overworld. Challenge worlds left for manual cleanup:");
        System.out.println("[SharedHealth] /mw delete " + currentChallengeWorld);
        System.out.println("[SharedHealth] /mw delete " + currentChallengeWorld + "_nether");
        System.out.println("[SharedHealth] /mw delete " + currentChallengeWorld + "_the_end");

        currentChallengeWorld = null;
    }

    public boolean isCountingDown() {
        return isCountingDown;
    }

    public String getCurrentChallengeWorld() {
        return currentChallengeWorld;
    }
}