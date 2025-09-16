package dev.neddslayer.sharedhealth;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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

        // Stop the server when countdown reaches 0
        if (shutdownTicks <= 0) {
            server.stop(false);
        }
    }

    public boolean isShuttingDown() {
        return isShuttingDown;
    }
}