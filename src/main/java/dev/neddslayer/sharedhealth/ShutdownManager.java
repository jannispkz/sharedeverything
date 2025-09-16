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
    private static final int SHUTDOWN_DELAY = 600; // 30 seconds * 20 ticks

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

        // Only update the display every 20 ticks (1 second) to avoid spam
        if (shutdownTicks % 20 == 0 || shutdownTicks <= 20) {
            // Send red countdown to all players
            Text shutdownMessage = Text.literal("Server stopping in: " + secondsLeft).formatted(Formatting.RED, Formatting.BOLD);
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

    private void deleteWorldAndStop() {
        // Kick all players with reset message
        Text kickMessage = Text.literal("World is resetting, rejoin in like 15 seconds").formatted(net.minecraft.util.Formatting.YELLOW);
        for (net.minecraft.server.network.ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.disconnect(kickMessage);
        }

        // Save the server first
        server.save(false, true, true);

        // Schedule world deletion after server stops
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // Small delay to ensure server is fully stopped
                Thread.sleep(2000);

                // Delete the world folder
                File worldFolder = new File("world");
                if (worldFolder.exists() && worldFolder.isDirectory()) {
                    deleteDirectoryRecursively(worldFolder.toPath());
                    System.out.println("[SharedHealth] World folder deleted successfully.");
                }
            } catch (Exception e) {
                System.err.println("[SharedHealth] Failed to delete world: " + e.getMessage());
            }
        }));

        // Stop the server
        server.stop(false);
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