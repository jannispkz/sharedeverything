package dev.neddslayer.sharedhealth;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class ShutdownManager {
    private final MinecraftServer server;
    private boolean isShuttingDown;
    private int shutdownTicks;
    private static final int SHUTDOWN_DELAY = 300; // 15 seconds * 20 ticks

    public ShutdownManager(MinecraftServer server) {
        this.server = server;
        this.isShuttingDown = false;
        this.shutdownTicks = 0;
    }

    public void startShutdown() {
        this.isShuttingDown = true;
        this.shutdownTicks = SHUTDOWN_DELAY;
    }

    public void tick() {
        if (!isShuttingDown) {
            return;
        }

        shutdownTicks--;

        // Calculate seconds remaining
        int secondsLeft = (shutdownTicks + 19) / 20; // Round up

        // Send red countdown to all players
        Text shutdownMessage = Text.literal("Server stopping in: " + secondsLeft).formatted(Formatting.RED, Formatting.BOLD);
        server.getPlayerManager().getPlayerList().forEach(player -> {
            player.sendMessage(shutdownMessage, true);
        });

        // Stop the server and delete world when countdown reaches 0
        if (shutdownTicks <= 0) {
            // Save and stop the server first
            server.save(false, true, true);

            // Schedule world deletion after server stops
            scheduleWorldDeletion();

            // Stop the server
            server.stop(false);
        }
    }

    public boolean isShuttingDown() {
        return isShuttingDown;
    }

    private void scheduleWorldDeletion() {
        // Get the world folder
        File worldFolder = new File("world");

        // Create a shutdown hook to delete the world after the server stops
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // Small delay to ensure server is fully stopped
                Thread.sleep(1000);

                // Delete the world folder recursively
                if (worldFolder.exists() && worldFolder.isDirectory()) {
                    deleteDirectoryRecursively(worldFolder.toPath());
                    System.out.println("[SharedHealth] World folder deleted successfully.");
                }

                // Also delete DIM folders if they exist
                File netherFolder = new File("world/DIM-1");
                if (netherFolder.exists()) {
                    deleteDirectoryRecursively(netherFolder.toPath());
                }

                File endFolder = new File("world/DIM1");
                if (endFolder.exists()) {
                    deleteDirectoryRecursively(endFolder.toPath());
                }
            } catch (Exception e) {
                System.err.println("[SharedHealth] Failed to delete world: " + e.getMessage());
            }
        }));
    }

    private void deleteDirectoryRecursively(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}