package dev.neddslayer.sharedhealth;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
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

        // Send title and play sound to all players
        Text title = Text.literal("Timer lööft").formatted(Formatting.GREEN);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // Show title using network handler
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(title));
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(Text.empty()));
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(10, 70, 20));

            // Play cat meow sound
            player.playSoundToPlayer(SoundEvents.ENTITY_CAT_AMBIENT, SoundCategory.MASTER, 1.0f, 1.0f);
        }
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
        Text message = Text.literal(timeString).formatted(Formatting.WHITE);

        server.getPlayerManager().getPlayerList().forEach(player -> {
            player.sendMessage(message, true);
        });
    }
}