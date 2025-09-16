package dev.neddslayer.sharedhealth;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class CountdownManager {
    private final MinecraftServer server;
    private long startTime;
    private boolean isActive;

    public CountdownManager(MinecraftServer server) {
        this.server = server;
        this.startTime = 0;
        this.isActive = false;
    }

    public void start() {
        this.startTime = System.currentTimeMillis();
        this.isActive = true;
    }

    public void stop() {
        this.isActive = false;
    }

    public boolean isActive() {
        return isActive;
    }

    public void tick() {
        if (!isActive) {
            return;
        }

        long elapsedMillis = System.currentTimeMillis() - startTime;
        long totalSeconds = elapsedMillis / 1000;

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        String timeString = String.format("%d:%02d:%02d", hours, minutes, seconds);
        Text message = Text.literal("Timer: " + timeString).formatted(Formatting.YELLOW);

        server.getPlayerManager().getPlayerList().forEach(player -> {
            player.sendMessage(message, true);
        });
    }
}