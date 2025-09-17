package dev.neddslayer.sharedhealth;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.io.File;

public class ShutdownManager {
    private final MinecraftServer server;
    private boolean isShuttingDown;
    private boolean isVictoryShutdown = false;
    private int shutdownTicks;
    private static final int SHUTDOWN_DELAY = 600; // 30 seconds * 20 ticks

    public ShutdownManager(MinecraftServer server) {
        this.server = server;
        this.isShuttingDown = false;
        this.shutdownTicks = 0;
    }

    public void startShutdown() {
        this.isShuttingDown = true;
        this.shutdownTicks = SHUTDOWN_DELAY;
        this.isVictoryShutdown = false;
    }

    public void startVictoryShutdown() {
        this.isShuttingDown = true;
        this.shutdownTicks = SHUTDOWN_DELAY;
        this.isVictoryShutdown = true;
    }

    public void tick() {
        if (!isShuttingDown) {
            return;
        }

        shutdownTicks--;

        // Calculate seconds remaining
        int secondsLeft = (shutdownTicks + 19) / 20; // Round up

        // Only update the display every 20 ticks (1 second) to avoid spam
        if (shutdownTicks % 20 == 0 || shutdownTicks <= 20) {
            // Send red countdown to all players
            Text shutdownMessage = Text.literal("World resetting in: " + secondsLeft).formatted(Formatting.RED, Formatting.BOLD);
            server.getPlayerManager().getPlayerList().forEach(player -> {
                player.sendMessage(shutdownMessage, true);
            });
        }

        // Delete world and stop server when countdown reaches 0
        if (shutdownTicks <= 0) {
            deleteWorldAndStop();
        }
    }

    public boolean isShuttingDown() {
        return isShuttingDown;
    }

    public void forceImmediateShutdown() {
        this.isShuttingDown = true;
        this.isVictoryShutdown = false;
        deleteWorldAndStop();
    }

    public void forceImmediateVictoryShutdown() {
        this.isShuttingDown = true;
        this.isVictoryShutdown = true;
        deleteWorldAndStop();
    }

    private void deleteWorldAndStop() {
        // Kick all players with reset message
        Text kickMessage = Text.literal("World is resetting, rejoin in like 15 seconds").formatted(net.minecraft.util.Formatting.YELLOW);
        for (net.minecraft.server.network.ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.disconnect(kickMessage);
        }

        // Save the server first
        server.save(false, true, true);

        // Schedule world renaming after server stops
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // Small delay to ensure server is fully stopped
                Thread.sleep(2000);

                File worldFolder = new File("world");
                if (worldFolder.exists() && worldFolder.isDirectory()) {
                    if (isVictoryShutdown) {
                        // Victory: Rename the world folder with timestamp
                        long timestamp = System.currentTimeMillis() / 1000; // Unix timestamp in seconds
                        File renamedFolder = new File("victory_" + timestamp);

                        if (worldFolder.renameTo(renamedFolder)) {
                            System.out.println("[SharedHealth] Victory! World folder renamed to: " + renamedFolder.getName());

                            // Create a fresh empty world folder for next game
                            File newWorldFolder = new File("world");
                            newWorldFolder.mkdirs();
                            System.out.println("[SharedHealth] Created new empty world folder for next game.");
                        } else {
                            System.err.println("[SharedHealth] Failed to rename world folder.");
                        }
                    } else {
                        // Death/Reset: Delete the world folder completely
                        deleteDirectoryRecursively(worldFolder);
                        System.out.println("[SharedHealth] World folder deleted (death/reset).");
                    }
                }
            } catch (Exception e) {
                System.err.println("[SharedHealth] Failed to rename world: " + e.getMessage());
            }
        }));

        // Stop the server
        server.stop(false);
    }

    private void deleteDirectoryRecursively(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectoryRecursively(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}